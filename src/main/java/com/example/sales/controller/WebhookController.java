// File: com/example/sales/controller/WebhookController.java
package com.example.sales.controller;

import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.Shop;
import com.example.sales.service.PaymentService;
import com.example.sales.service.ShopService;
import com.example.sales.util.SignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ShopService shopService;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Value("${webhook.secret}")
    private String webhookSecret;

    @PostMapping("/payment")
    public void handlePaymentWebhook(@RequestBody String rawBody,
                                     @RequestHeader("X-Payment-Signature") String signature) {
        log.info("Received payment webhook");

        try {
            // ✅ 1. Verify signature
            if (!SignatureUtil.isValidHmac(rawBody, signature, webhookSecret)) {
                log.warn("Invalid webhook signature");
                return;
            }

            // ✅ 2. Parse JSON payload
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            String shopId = (String) payload.get("shopId");
            String planStr = (String) payload.get("plan");
            Integer months = (Integer) payload.get("months");

            if (shopId == null || planStr == null || months == null) {
                log.warn("Missing required fields in webhook payload");
                return;
            }

            SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(planStr);
            Shop shop = shopService.getShopById(shopId);

            // ✅ 3. Upgrade plan
            paymentService.upgradeShopPlan(shop, targetPlan, months);
            shopService.save(shop);

            log.info("✅ Shop {} upgraded to {} for {} months", shopId, targetPlan, months);
        } catch (Exception e) {
            log.error("Error processing webhook", e);
        }
    }
}
