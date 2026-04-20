// File: src/main/java/com/example/sales/controller/admin/AdminDashboardController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.admin.AdminDashboardResponse;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.admin.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KPI tổng quan cho trang quản trị.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin — Dashboard", description = "KPI tổng quan hệ thống")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "KPI tổng quan (cache 60s)")
    @RequireAdminPermission(AdminPermission.DASHBOARD_VIEW)
    public ApiResponseDto<AdminDashboardResponse> overview() {
        return ApiResponseDto.success(ApiCode.SUCCESS, dashboardService.overview());
    }
}
