// File: src/main/java/com/example/sales/controller/admin/AdminImpersonationController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.admin.AdminImpersonationResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import com.example.sales.security.Audited;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.JwtUtil;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.audit.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Issue short-lived impersonation tokens. Admin với quyền IMPERSONATE có thể
 * lấy JWT giả danh user mục tiêu (1h) để debug. Token mang claim
 * {@code impersonatedBy} để FE hiển thị banner và BE ghi audit.
 */
@RestController
@RequestMapping("/api/admin/impersonate")
@RequiredArgsConstructor
@Tag(name = "Admin — Impersonation", description = "Giả danh user để debug (ngắn hạn)")
public class AdminImpersonationController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AdminAuditService adminAuditService;

    @PostMapping("/{userId}")
    @Operation(summary = "Issue impersonation JWT (1h) cho user mục tiêu")
    @RequireAdminPermission(AdminPermission.IMPERSONATE)
    @Audited(resource = "AUTH", action = "IMPERSONATE_START", targetIdExpr = "#userId")
    public ApiResponseDto<AdminImpersonationResponse> start(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        User target = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        if (!target.isActive()) {
            throw new BusinessException(ApiCode.USER_NOT_FOUND);
        }

        String adminId = admin != null ? admin.getId() : null;
        String adminEmail = admin != null ? admin.getEmail() : null;

        String token = jwtUtil.generateImpersonationToken(target, adminId, adminEmail);

        adminAuditService.record(
                adminId, adminEmail, "AUTH", "IMPERSONATE_ISSUED",
                target.getId(),
                Map.of(
                        "targetEmail", target.getEmail(),
                        "targetRole", target.getRole().name()
                ),
                true, null
        );

        return ApiResponseDto.success(ApiCode.SUCCESS, AdminImpersonationResponse.builder()
                .accessToken(token)
                .expiresInSeconds(3600L)
                .targetUserId(target.getId())
                .targetEmail(target.getEmail())
                .targetRole(target.getRole().name())
                .build());
    }
}
