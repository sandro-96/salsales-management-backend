// File: src/main/java/com/example/sales/controller/admin/AdminUserController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.UserRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.admin.AdminUserDetail;
import com.example.sales.dto.admin.AdminUserPermissionsRequest;
import com.example.sales.dto.admin.AdminUserStatusUpdateRequest;
import com.example.sales.dto.admin.AdminUserSummary;
import com.example.sales.security.Audited;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị user toàn hệ thống (read-only trong Phase 1).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — Users", description = "Quản trị user hệ thống")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Danh sách user (filter role/status/keyword)")
    @RequireAdminPermission(AdminPermission.USER_VIEW)
    public ApiResponseDto<Page<AdminUserSummary>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminUserService.list(pageable, role, status, keyword));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Chi tiết user + shop memberships")
    @RequireAdminPermission(AdminPermission.USER_VIEW)
    public ApiResponseDto<AdminUserDetail> detail(@PathVariable String userId) {
        return ApiResponseDto.success(ApiCode.SUCCESS, adminUserService.detail(userId));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Khoá / mở khoá user")
    @RequireAdminPermission(AdminPermission.USER_MANAGE)
    @Audited(resource = "USER", action = "UPDATE_STATUS", targetIdExpr = "#userId")
    public ApiResponseDto<AdminUserSummary> updateStatus(
            @PathVariable String userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminUserService.updateStatus(userId, req, admin != null ? admin.getId() : null));
    }

    @PatchMapping("/{userId}/admin-permissions")
    @Operation(summary = "Đặt tập AdminPermission cho user ROLE_ADMIN (preset hoặc custom)")
    @RequireAdminPermission(AdminPermission.USER_MANAGE)
    @Audited(resource = "USER", action = "UPDATE_ADMIN_PERMISSIONS", targetIdExpr = "#userId")
    public ApiResponseDto<AdminUserSummary> updateAdminPermissions(
            @PathVariable String userId,
            @Valid @RequestBody AdminUserPermissionsRequest req,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminUserService.updateAdminPermissions(userId, req, admin != null ? admin.getId() : null));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Admin khởi tạo reset password + gửi mail")
    @RequireAdminPermission(AdminPermission.USER_MANAGE)
    @Audited(resource = "USER", action = "RESET_PASSWORD", targetIdExpr = "#userId")
    public ApiResponseDto<Void> resetPassword(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        adminUserService.resetPassword(userId, admin != null ? admin.getId() : null);
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    @PostMapping("/{userId}/resend-verify")
    @Operation(summary = "Admin gửi lại email xác thực")
    @RequireAdminPermission(AdminPermission.USER_MANAGE)
    @Audited(resource = "USER", action = "RESEND_VERIFY", targetIdExpr = "#userId")
    public ApiResponseDto<Void> resendVerification(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        adminUserService.resendVerification(userId, admin != null ? admin.getId() : null);
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }
}
