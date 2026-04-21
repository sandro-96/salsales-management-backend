// File: src/main/java/com/example/sales/controller/SubscriptionController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.subscription.SubscriptionDto;
import com.example.sales.dto.subscription.SubscriptionManualTransferReportRequest;
import com.example.sales.dto.subscription.SubscriptionPayRequest;
import com.example.sales.dto.subscription.SubscriptionPayResponse;
import com.example.sales.dto.subscription.SubscriptionTransferInstructionsDto;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.security.Audited;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.BillingTransferInfoService;
import com.example.sales.service.ShopContextResolver;
import com.example.sales.service.SubscriptionService;
import com.example.sales.service.payment.PaymentGateway;
import com.example.sales.service.payment.PaymentGatewayRegistry;
import com.example.sales.service.payment.PaymentInitiation;
import com.example.sales.service.payment.PaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ShopContextResolver shopContextResolver;
    private final SubscriptionService subscriptionService;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final BillingTransferInfoService billingTransferInfoService;

    @GetMapping("/me")
    @Operation(summary = "Lấy trạng thái subscription hiện tại",
            description = "Theo đúng shop: query shopId hoặc header X-Shop-Id (bắt buộc nếu user gắn nhiều shop — chủ hoặc được mời). "
                    + "TRIAL/ACTIVE/EXPIRED, ngày còn lại, nextBillingDate, 99.000đ/kỳ.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Không xác thực")
    })
    public ApiResponseDto<SubscriptionDto> getCurrentSubscription(
            @AuthenticationPrincipal @Parameter(description = "Người dùng hiện tại") CustomUserDetails user,
            HttpServletRequest request) {
        String hint = ShopContextResolver.shopIdHintFrom(request);
        Shop shop = shopContextResolver.resolveShopForSubscription(user.getId(), hint);
        Subscription sub = subscriptionService.ensureSubscription(shop);
        return ApiResponseDto.success(ApiCode.SUCCESS, subscriptionService.toDto(sub));
    }

    @GetMapping("/transfer-info")
    @Operation(summary = "Thông tin tài khoản/QR chuyển khoản hệ thống (chưa tạo mã giao dịch)",
            description = "Dùng để hiển thị số TK + hướng dẫn trước khi shop bấm Thanh toán. "
                    + "Nếu chưa cấu hình app.billing.transfer thì trả data null.")
    public ApiResponseDto<SubscriptionTransferInstructionsDto> getTransferInfo(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest request) {
        String hint = ShopContextResolver.shopIdHintFrom(request);
        Shop shop = shopContextResolver.resolveShopForSubscription(user.getId(), hint);
        Subscription sub = subscriptionService.ensureSubscription(shop);
        SubscriptionTransferInstructionsDto dto =
                billingTransferInfoService.buildStaticPreview(sub.getAmountVnd());
        return ApiResponseDto.success(ApiCode.SUCCESS, dto);
    }

    @PostMapping("/pay")
    @Operation(summary = "Khởi tạo thanh toán gia hạn",
            description = "Gọi payment gateway (MANUAL/VNPAY/MOMO) và trả về URL redirect + transactionId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo giao dịch thành công"),
            @ApiResponse(responseCode = "401", description = "Không xác thực")
    })
    @Audited(resource = "SUBSCRIPTION", action = "PAY_INIT")
    public ApiResponseDto<SubscriptionPayResponse> pay(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody(required = false) SubscriptionPayRequest req,
            HttpServletRequest request) {
        String hint = ShopContextResolver.shopIdHintFrom(request);
        Shop shop = shopContextResolver.resolveShopForSubscription(user.getId(), hint);
        Subscription sub = subscriptionService.ensureSubscription(shop);

        PaymentGatewayType requested = req != null ? req.getGateway() : null;
        PaymentGateway gateway = gatewayRegistry.resolve(requested);
        PaymentInitiation init = gateway.initiatePayment(PaymentRequest.builder()
                .shopId(shop.getId())
                .ownerId(shop.getOwnerId())
                .amountVnd(sub.getAmountVnd())
                .description("Gia han goi BASIC cho shop " + shop.getName())
                .returnUrl(req != null ? req.getReturnUrl() : null)
                .build());

        log.info("[Subscription/pay] shop={} owner={} gateway={} amount={}đ txnRef={}",
                shop.getId(), shop.getOwnerId(), init.getGateway(), init.getAmountVnd(), init.getTransactionId());

        SubscriptionTransferInstructionsDto transfer = null;
        if (init.getGateway() == PaymentGatewayType.MANUAL) {
            transfer = billingTransferInfoService.buildForPayment(
                    init.getTransactionId(), shop.getId(), shop.getName(), init.getAmountVnd());
            subscriptionService.notifyAdminsManualTransferPending(
                    shop, init.getTransactionId(), init.getAmountVnd());
        }

        return ApiResponseDto.success(ApiCode.SUCCESS, SubscriptionPayResponse.builder()
                .gateway(init.getGateway())
                .paymentUrl(init.getPaymentUrl())
                .transactionId(init.getTransactionId())
                .amountVnd(init.getAmountVnd())
                .transferInstructions(transfer)
                .build());
    }

    @GetMapping("/history")
    @Operation(summary = "Lịch sử thanh toán / thay đổi gói",
            description = "Các bản ghi PAYMENT/ADMIN_EXTEND/EXPIRED của shop, mới nhất trước.")
    public ApiResponseDto<List<SubscriptionHistory>> getHistory(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest request) {
        String hint = ShopContextResolver.shopIdHintFrom(request);
        Shop shop = shopContextResolver.resolveShopForSubscription(user.getId(), hint);
        List<SubscriptionHistory> history = subscriptionHistoryRepository
                .findByShopIdOrderByCreatedAtDesc(shop.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, history);
    }

    @PostMapping("/manual-transfer/reported")
    @Operation(summary = "Shop báo đã chuyển khoản",
            description = "Ghi nhận shop đã CK (MANUAL PENDING). Không thay status — admin vẫn phải xác nhận.")
    @Audited(resource = "SUBSCRIPTION", action = "MANUAL_TRANSFER_REPORTED")
    public ApiResponseDto<SubscriptionDto> reportManualTransferSent(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody(required = false) SubscriptionManualTransferReportRequest body,
            HttpServletRequest request) {
        String hint = ShopContextResolver.shopIdHintFrom(request);
        Shop shop = shopContextResolver.resolveShopForSubscription(user.getId(), hint);
        String ref = body != null ? body.getProviderTxnRef() : null;
        subscriptionService.reportShopManualTransferSent(shop.getId(), user.getId(), ref);
        Subscription sub = subscriptionService.ensureSubscription(shop);
        return ApiResponseDto.success(ApiCode.SUCCESS, subscriptionService.toDto(sub));
    }
}
