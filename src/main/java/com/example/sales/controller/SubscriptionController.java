// File: com/example/sales/controller/SubscriptionController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.subscription.UpgradePlanRequest;
import com.example.sales.model.Shop;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.PaymentService;
import com.example.sales.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ShopService shopService;
    private final PaymentService paymentService;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;

    @GetMapping("/me")
    public ApiResponseDto<Shop> getCurrentPlan(@AuthenticationPrincipal CustomUserDetails user) {
        Shop shop = shopService.getShopByOwner(user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, shop);
    }

    @PostMapping("/upgrade")
    public ApiResponseDto<?> upgrade(@AuthenticationPrincipal CustomUserDetails user,
                                     @RequestBody @Valid UpgradePlanRequest req) {
        Shop shop = shopService.getShopByOwner(user.getId());

        paymentService.upgradeShopPlan(shop, req.getTargetPlan(), req.getMonths());

        shopService.save(shop); // hoặc updateShop()

        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @GetMapping("/history")
    public ApiResponseDto<List<SubscriptionHistory>> getHistory(@AuthenticationPrincipal CustomUserDetails user) {
        Shop shop = shopService.getShopByOwner(user.getId());
        List<SubscriptionHistory> history = subscriptionHistoryRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, history);
    }
}
