// File: src/main/java/com/example/sales/controller/admin/AdminMeController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Set;

/**
 * Thông tin admin hiện tại (đã login). Dùng cho FE để biết render nav, ẩn
 * action theo {@link AdminPermission}.
 */
@RestController
@RequestMapping("/api/admin/me")
@RequiredArgsConstructor
@Tag(name = "Admin — Me", description = "Thông tin admin hiện tại (permissions, presets)")
public class AdminMeController {

    @GetMapping("/permissions")
    @Operation(summary = "Lấy tập AdminPermission của admin hiện tại")
    public ApiResponseDto<PermissionsResponse> me(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Set<AdminPermission> perms = user != null && user.getAdminPermissions() != null
                ? EnumSet.copyOf(user.getAdminPermissions())
                : EnumSet.noneOf(AdminPermission.class);
        return ApiResponseDto.success(ApiCode.SUCCESS, new PermissionsResponse(
                user != null ? user.getId() : null,
                user != null ? user.getEmail() : null,
                perms
        ));
    }

    public record PermissionsResponse(String userId, String email, Set<AdminPermission> permissions) {
    }
}
