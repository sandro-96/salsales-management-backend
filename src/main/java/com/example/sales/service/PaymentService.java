// File: src/main/java/com/example/sales/service/PaymentService.java
package com.example.sales.service;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.Shop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Facade billing cho các controller cũ — toàn bộ logic state đã được chuyển sang
 * {@link SubscriptionService}. Giữ class này để các endpoint legacy (WebhookController,
 * SubscriptionController#upgrade) không break khi refactor sang Subscription model mới.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionService subscriptionService;

    /**
     * @deprecated dùng {@link SubscriptionService#recordPayment(String, String, PaymentGatewayType, String)}.
     *     Giữ để tương thích webhook cũ (payload mang tới {@code plan}/{@code months} nhưng giờ bỏ qua).
     */
    @Deprecated
    public void upgradeShopPlan(Shop shop, SubscriptionPlan newPlan, int months) {
        log.info("[PaymentService] legacy upgradeShopPlan shop={} plan={} months={} — "
                + "đã được remap thành recordPayment cho subscription model mới.",
                shop.getId(), newPlan, months);
        subscriptionService.recordPayment(shop.getId(), null, PaymentGatewayType.MANUAL, shop.getOwnerId());
    }
}
