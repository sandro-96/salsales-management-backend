// File: src/main/java/com/example/sales/controller/admin/AdminBillingController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.admin.AdminBillingOverviewResponse;
import com.example.sales.dto.admin.AdminPaymentTransactionItem;
import com.example.sales.dto.admin.AdminPaymentTransactionResolveRequest;
import com.example.sales.dto.admin.AdminPaymentTransactionResyncResponse;
import com.example.sales.dto.admin.AdminSubscriptionHistoryItem;
import com.example.sales.security.Audited;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.admin.AdminBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/billing")
@RequiredArgsConstructor
@Tag(name = "Admin — Billing", description = "Thống kê doanh thu & lịch sử subscription")
public class AdminBillingController {

    private final AdminBillingService billingService;

    @GetMapping("/overview")
    @Operation(summary = "MRR snapshot + trend theo tháng (cache 60s)")
    @RequireAdminPermission(AdminPermission.BILLING_VIEW)
    public ApiResponseDto<AdminBillingOverviewResponse> overview(
            @RequestParam(name = "months", defaultValue = "6") int months
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, billingService.overview(months));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Danh sách SubscriptionHistory có filter + paging")
    @RequireAdminPermission(AdminPermission.BILLING_VIEW)
    public ApiResponseDto<Page<AdminSubscriptionHistoryItem>> subscriptions(
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) SubscriptionActionType actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                billingService.subscriptions(shopId, actionType, from, to, pageable));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Danh sách PaymentTransaction (trace gateway, phục vụ debug IPN)")
    @RequireAdminPermission(AdminPermission.BILLING_VIEW)
    public ApiResponseDto<Page<AdminPaymentTransactionItem>> paymentTransactions(
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) PaymentTransactionStatus status,
            @RequestParam(required = false) PaymentGatewayType gateway,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                billingService.paymentTransactions(shopId, status, gateway, from, to, pageable));
    }

    @PostMapping("/transactions/{id}/resync")
    @Operation(summary = "Resync PaymentTransaction với gateway (VNPay Querydr / MoMo query)",
            description = "Gọi gateway để check trạng thái thực tế. "
                    + "Nếu gateway báo SUCCESS → ghi nhận thanh toán (idempotent). "
                    + "Nếu FAILED → đóng txn. Nếu PENDING/UNKNOWN → không thay đổi DB.")
    @RequireAdminPermission(AdminPermission.BILLING_MANAGE)
    @Audited(resource = "PAYMENT_TRANSACTION", action = "ADMIN_RESYNC", targetIdExpr = "#id")
    public ApiResponseDto<AdminPaymentTransactionResyncResponse> resyncTransaction(
            @PathVariable String id
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                billingService.resyncPaymentTransaction(id));
    }

    @PostMapping("/transactions/{id}/resolve")
    @Operation(summary = "Đánh dấu PaymentTransaction PENDING là CANCELLED/FAILED (admin thủ công)",
            description = "Dùng cho txn treo lâu không có IPN. Không cho set SUCCESS — để tránh lệch đối soát gateway, "
                    + "nếu cần bù thời hạn hãy dùng endpoint gia hạn shop thay vì đổi trạng thái txn.")
    @RequireAdminPermission(AdminPermission.BILLING_MANAGE)
    @Audited(resource = "PAYMENT_TRANSACTION", action = "ADMIN_RESOLVE", targetIdExpr = "#id")
    public ApiResponseDto<AdminPaymentTransactionItem> resolveTransaction(
            @PathVariable String id,
            @Valid @RequestBody AdminPaymentTransactionResolveRequest body
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                billingService.resolvePaymentTransaction(id, body.getStatus(), body.getReason()));
    }
}
