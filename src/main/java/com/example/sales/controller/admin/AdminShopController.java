// File: src/main/java/com/example/sales/controller/admin/AdminShopController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.admin.AdminShopDetail;
import com.example.sales.dto.admin.AdminShopMarkPaidRequest;
import com.example.sales.dto.admin.AdminShopPlanExtendRequest;
import com.example.sales.dto.admin.AdminShopPlanUpdateRequest;
import com.example.sales.dto.admin.AdminShopStatusUpdateRequest;
import com.example.sales.dto.admin.AdminShopSummary;
import com.example.sales.security.Audited;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.admin.AdminShopService;
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
 * Quản trị shop toàn hệ thống (read-only trong Phase 1).
 */
@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
@Tag(name = "Admin — Shops", description = "Quản trị shop toàn hệ thống")
public class AdminShopController {

    private final AdminShopService adminShopService;

    @GetMapping
    @Operation(summary = "Danh sách shop (filter status/subscription/keyword)")
    @RequireAdminPermission(AdminPermission.SHOP_VIEW)
    public ApiResponseDto<Page<AdminShopSummary>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) SubscriptionStatus subscriptionStatus,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminShopService.list(pageable, status, subscriptionStatus, keyword));
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Chi tiết shop (kèm stats + lịch sử subscription)")
    @RequireAdminPermission(AdminPermission.SHOP_VIEW)
    public ApiResponseDto<AdminShopDetail> detail(@PathVariable String shopId) {
        return ApiResponseDto.success(ApiCode.SUCCESS, adminShopService.detail(shopId));
    }

    @PatchMapping("/{shopId}/status")
    @Operation(summary = "Khoá / mở khoá shop")
    @RequireAdminPermission(AdminPermission.SHOP_MANAGE)
    @Audited(resource = "SHOP", action = "UPDATE_STATUS", targetIdExpr = "#shopId")
    public ApiResponseDto<AdminShopSummary> updateStatus(
            @PathVariable String shopId,
            @Valid @RequestBody AdminShopStatusUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminShopService.updateStatus(shopId, req, admin != null ? admin.getId() : null));
    }

    @PatchMapping("/{shopId}/plan")
    @Operation(summary = "Đổi plan + cập nhật hạn; ghi SubscriptionHistory và notify owner")
    @RequireAdminPermission(AdminPermission.BILLING_MANAGE)
    @Audited(resource = "SHOP", action = "UPDATE_PLAN", targetIdExpr = "#shopId")
    public ApiResponseDto<AdminShopDetail> updatePlan(
            @PathVariable String shopId,
            @Valid @RequestBody AdminShopPlanUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminShopService.updatePlan(shopId, req, admin != null ? admin.getId() : null));
    }

    @PostMapping("/{shopId}/plan/extend")
    @Operation(summary = "Gia hạn subscription thêm N tháng (không thanh toán)")
    @RequireAdminPermission(AdminPermission.BILLING_MANAGE)
    @Audited(resource = "SHOP", action = "EXTEND_PLAN", targetIdExpr = "#shopId")
    public ApiResponseDto<AdminShopDetail> extendPlan(
            @PathVariable String shopId,
            @Valid @RequestBody AdminShopPlanExtendRequest req,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminShopService.extendPlan(shopId, req, admin != null ? admin.getId() : null));
    }

    @PatchMapping("/{shopId}/subscription-status")
    @Operation(summary = "Override subscription status (TRIAL/ACTIVE/EXPIRED/CANCELLED)",
            description = "Dùng khi cần can thiệp thủ công status; không thay đổi mốc thời gian.")
    @RequireAdminPermission(AdminPermission.BILLING_MANAGE)
    @Audited(resource = "SHOP", action = "OVERRIDE_SUBSCRIPTION_STATUS", targetIdExpr = "#shopId")
    public ApiResponseDto<AdminShopDetail> overrideSubscriptionStatus(
            @PathVariable String shopId,
            @RequestParam SubscriptionStatus status,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminShopService.overrideStatus(shopId, status, reason, admin != null ? admin.getId() : null));
    }

    @PostMapping("/{shopId}/plan/mark-paid")
    @Operation(summary = "Xác nhận đã nhận thanh toán 99.000đ (manual) và kéo dài 1 chu kỳ")
    @RequireAdminPermission(AdminPermission.BILLING_MANAGE)
    @Audited(resource = "SHOP", action = "MARK_PAID", targetIdExpr = "#shopId")
    public ApiResponseDto<AdminShopDetail> markPaid(
            @PathVariable String shopId,
            @RequestBody(required = false) AdminShopMarkPaidRequest req,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                adminShopService.markPaid(shopId, req, admin != null ? admin.getId() : null));
    }
}
