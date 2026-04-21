// File: src/main/java/com/example/sales/controller/WebhookController.java
package com.example.sales.controller;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.PaymentTransaction;
import com.example.sales.repository.PaymentTransactionRepository;
import com.example.sales.service.SubscriptionService;
import com.example.sales.service.admin.AdminBillingService;
import com.example.sales.service.payment.MoMoGateway;
import com.example.sales.service.payment.VNPayGateway;
import com.example.sales.util.SignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webhook / IPN cho payment gateway. Mỗi gateway có endpoint riêng:
 * <ul>
 *   <li>{@code GET /api/webhook/vnpay} — VNPay IPN (query params, HMAC-SHA512).</li>
 *   <li>{@code POST /api/webhook/momo}  — MoMo IPN (JSON body, HMAC-SHA256).</li>
 *   <li>{@code POST /api/webhook/payment} — legacy MANUAL / stub webhook, verify HMAC-SHA256 base64
 *       qua {@code webhook.secret}.</li>
 * </ul>
 * <p>Idempotency: mỗi provider txn ref map 1-1 với {@link PaymentTransaction};
 * nếu đã SUCCESS thì không gọi lại {@code recordPayment}.
 */
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;
    private final PaymentTransactionRepository txnRepository;
    private final VNPayGateway vnPayGateway;
    private final MoMoGateway momoGateway;
    private final AdminBillingService adminBillingService;

    @Value("${webhook.secret}")
    private String webhookSecret;

    // ─── VNPay IPN ─────────────────────────────────────────────────────
    // VNPay gọi GET tới ipnUrl với đủ query params đã ký HMAC-SHA512.
    // Response spec: {"RspCode":"00","Message":"Confirm Success"} (hoặc mã lỗi).
    @GetMapping("/vnpay")
    public Map<String, String> handleVnpayIpn(HttpServletRequest request) {
        Map<String, String> params = readQueryParams(request);
        log.info("[Webhook/VNPay] nhận IPN keys={}", params.keySet());

        VNPayGateway.VerifyResult verify = vnPayGateway.verifyReturn(params);
        if (!verify.valid()) {
            log.warn("[Webhook/VNPay] signature invalid: {}", verify.errorMessage());
            return vnpayResponse("97", "Invalid signature");
        }
        String txnRef = verify.txnRef();
        if (txnRef == null || txnRef.isBlank()) {
            return vnpayResponse("01", "Missing TxnRef");
        }

        PaymentTransaction txn = txnRepository.findByProviderTxnRef(txnRef).orElse(null);
        if (txn == null) {
            return vnpayResponse("01", "Order not found");
        }
        if (txn.getStatus() == PaymentTransactionStatus.SUCCESS) {
            log.info("[Webhook/VNPay] txn {} đã xử lý trước đó — idempotent OK", txnRef);
            return vnpayResponse("02", "Order already confirmed");
        }
        if (txn.getAmountVnd() != verify.amountVnd()) {
            markFailed(txn, params, "amount mismatch: expected=" + txn.getAmountVnd()
                    + " received=" + verify.amountVnd());
            return vnpayResponse("04", "Invalid amount");
        }
        if (!verify.success()) {
            markFailed(txn, params, "responseCode=" + verify.responseCode());
            return vnpayResponse("00", "Confirm Success"); // vẫn ack để VNPay dừng retry
        }

        // Gọi recordPayment trước — nếu shop bị xoá / không tồn tại thì
        // mark FAILED thay vì để txn ở trạng thái SUCCESS lệch với subscription.
        try {
            subscriptionService.recordPayment(
                    txn.getShopId(),
                    verify.providerTransNo() != null ? verify.providerTransNo() : txnRef,
                    PaymentGatewayType.VNPAY,
                    null);
        } catch (BusinessException ex) {
            markFailed(txn, params, "recordPayment failed: " + ex.getError() + " " + ex.getMessage());
            return vnpayResponse("01", "Order not found");
        }

        txn.setProviderTransNo(verify.providerTransNo());
        txn.setStatus(PaymentTransactionStatus.SUCCESS);
        txn.setCompletedAt(LocalDateTime.now());
        txn.setRawCallback(stringify(params));
        txnRepository.save(txn);
        adminBillingService.invalidateOverviewCache();

        return vnpayResponse("00", "Confirm Success");
    }

    // ─── MoMo IPN ──────────────────────────────────────────────────────
    // MoMo gọi POST JSON tới ipnUrl. Body có signature HMAC-SHA256.
    @PostMapping("/momo")
    public ResponseEntity<Map<String, Object>> handleMomoIpn(@RequestBody Map<String, Object> payload) {
        log.info("[Webhook/MoMo] nhận IPN orderId={} resultCode={}",
                payload.get("orderId"), payload.get("resultCode"));

        MoMoGateway.VerifyResult verify = momoGateway.verifyIpn(payload);
        if (!verify.valid()) {
            log.warn("[Webhook/MoMo] signature invalid: {}", verify.message());
            return ResponseEntity.ok(Map.of("message", "invalid signature", "resultCode", 97));
        }
        String orderId = verify.orderId();
        if (orderId == null || orderId.isBlank()) {
            return ResponseEntity.ok(Map.of("message", "missing orderId", "resultCode", 1));
        }

        PaymentTransaction txn = txnRepository.findByProviderTxnRef(orderId).orElse(null);
        if (txn == null) {
            return ResponseEntity.ok(Map.of("message", "order not found", "resultCode", 1));
        }
        if (txn.getStatus() == PaymentTransactionStatus.SUCCESS) {
            log.info("[Webhook/MoMo] txn {} đã xử lý trước đó — idempotent OK", orderId);
            return ResponseEntity.ok(Map.of("message", "already confirmed", "resultCode", 0));
        }
        if (txn.getAmountVnd() != verify.amountVnd()) {
            markFailed(txn, payload, "amount mismatch: expected=" + txn.getAmountVnd()
                    + " received=" + verify.amountVnd());
            return ResponseEntity.ok(Map.of("message", "invalid amount", "resultCode", 4));
        }
        if (!verify.success()) {
            markFailed(txn, payload, "resultCode=" + verify.resultCode());
            return ResponseEntity.ok(Map.of("message", "acknowledged", "resultCode", 0));
        }

        try {
            subscriptionService.recordPayment(
                    txn.getShopId(),
                    verify.providerTransNo() != null ? verify.providerTransNo() : orderId,
                    PaymentGatewayType.MOMO,
                    null);
        } catch (BusinessException ex) {
            markFailed(txn, payload, "recordPayment failed: " + ex.getError() + " " + ex.getMessage());
            return ResponseEntity.ok(Map.of("message", "order not found", "resultCode", 1));
        }

        txn.setProviderTransNo(verify.providerTransNo());
        txn.setStatus(PaymentTransactionStatus.SUCCESS);
        txn.setCompletedAt(LocalDateTime.now());
        txn.setRawCallback(stringify(payload));
        txnRepository.save(txn);
        adminBillingService.invalidateOverviewCache();

        return ResponseEntity.ok(Map.of("message", "confirm success", "resultCode", 0));
    }

    // ─── Legacy MANUAL/stub webhook ────────────────────────────────────
    @PostMapping("/payment")
    public void handlePaymentWebhook(@RequestBody String rawBody,
                                     @RequestHeader("X-Payment-Signature") String signature) {
        log.info("Received payment webhook");

        try {
            if (!SignatureUtil.isValidHmac(rawBody, signature, webhookSecret)) {
                log.warn("Invalid webhook signature");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            String shopId = (String) payload.get("shopId");
            String transactionId = (String) payload.get("transactionId");
            String gatewayStr = (String) payload.get("gateway");

            if (shopId == null) {
                log.warn("Missing shopId in webhook payload");
                return;
            }

            PaymentGatewayType gateway = PaymentGatewayType.MANUAL;
            if (gatewayStr != null) {
                try {
                    gateway = PaymentGatewayType.valueOf(gatewayStr.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown gateway '{}' in webhook payload, fallback MANUAL", gatewayStr);
                }
            }

            subscriptionService.recordPayment(shopId, transactionId, gateway, null);
            adminBillingService.invalidateOverviewCache();
            log.info("✅ Subscription payment recorded for shop {} (tx={}, gw={})",
                    shopId, transactionId, gateway);
        } catch (Exception e) {
            log.error("Error processing webhook", e);
        }
    }

    // ─── helpers ───────────────────────────────────────────────────────
    private Map<String, String> readQueryParams(HttpServletRequest request) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            String[] values = e.getValue();
            if (values != null && values.length > 0) {
                out.put(e.getKey(), values[0]);
            }
        }
        return out;
    }

    private Map<String, String> vnpayResponse(String rspCode, String message) {
        Map<String, String> out = new HashMap<>();
        out.put("RspCode", rspCode);
        out.put("Message", message);
        return out;
    }

    private void markFailed(PaymentTransaction txn, Object rawPayload, String reason) {
        txn.setStatus(PaymentTransactionStatus.FAILED);
        txn.setFailureReason(reason);
        txn.setCompletedAt(LocalDateTime.now());
        txn.setRawCallback(stringify(rawPayload));
        txnRepository.save(txn);
        adminBillingService.invalidateOverviewCache();
        log.warn("[Webhook] txn {} đánh dấu FAILED: {}", txn.getProviderTxnRef(), reason);
    }

    private String stringify(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return String.valueOf(payload);
        }
    }
}
