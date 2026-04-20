// File: src/main/java/com/example/sales/controller/SubscriptionController.java
package com.example.sales.controller;

import com.example.sales.cache.ShopCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.subscription.SubscriptionDto;
import com.example.sales.dto.subscription.SubscriptionPayRequest;
import com.example.sales.dto.subscription.SubscriptionPayResponse;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.security.Audited;
import com.example.sales.security.CustomUserDetails;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ShopCache shopCache;
    private final SubscriptionService subscriptionService;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;

    @GetMapping("/me")
    @Operation(summary = "Lấy trạng thái subscription hiện tại",
            description = "Trả về TRIAL/ACTIVE/EXPIRED, số ngày còn lại, nextBillingDate và số tiền/kỳ (99.000đ).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Không xác thực")
    })
    public ApiResponseDto<SubscriptionDto> getCurrentSubscription(
            @AuthenticationPrincipal @Parameter(description = "Người dùng hiện tại") CustomUserDetails user) {
        Shop shop = shopCache.getShopByOwner(user.getId());
        Subscription sub = subscriptionService.ensureSubscription(shop);
        return ApiResponseDto.success(ApiCode.SUCCESS, subscriptionService.toDto(sub));
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
            @RequestBody(required = false) SubscriptionPayRequest req) {
        Shop shop = shopCache.getShopByOwner(user.getId());
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

        return ApiResponseDto.success(ApiCode.SUCCESS, SubscriptionPayResponse.builder()
                .gateway(init.getGateway())
                .paymentUrl(init.getPaymentUrl())
                .transactionId(init.getTransactionId())
                .amountVnd(init.getAmountVnd())
                .build());
    }

    @GetMapping("/history")
    @Operation(summary = "Lịch sử thanh toán / thay đổi gói",
            description = "Các bản ghi PAYMENT/ADMIN_EXTEND/EXPIRED của shop, mới nhất trước.")
    public ApiResponseDto<List<SubscriptionHistory>> getHistory(
            @AuthenticationPrincipal CustomUserDetails user) {
        Shop shop = shopCache.getShopByOwner(user.getId());
        List<SubscriptionHistory> history = subscriptionHistoryRepository
                .findByShopIdOrderByCreatedAtDesc(shop.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, history);
    }
}
