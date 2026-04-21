// File: src/main/java/com/example/sales/service/payment/ManualPaymentGateway.java
package com.example.sales.service.payment;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.model.PaymentTransaction;
import com.example.sales.repository.PaymentTransactionRepository;
import com.example.sales.service.admin.AdminBillingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gateway mặc định — dùng cho trường hợp admin/shop chuyển khoản thủ công.
 * Không tạo URL redirect thực; trả về transactionId tạm để admin có thể gắn vào khi
 * xác nhận thanh toán thủ công qua UI admin.
 * <p>
 * Lưu {@link PaymentTransaction} trạng thái PENDING để admin đối soát / dashboard đếm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualPaymentGateway implements PaymentGateway {

    private final PaymentTransactionRepository txnRepository;
    private final ObjectMapper objectMapper;
    /** Trì hoãn lookup — tránh vòng AdminBillingService → Registry → ManualPaymentGateway. */
    private final ObjectProvider<AdminBillingService> adminBillingService;

    @Override
    public PaymentGatewayType type() {
        return PaymentGatewayType.MANUAL;
    }

    @Override
    public PaymentInitiation initiatePayment(PaymentRequest request) {
        String tx = "MANUAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.info("[ManualPaymentGateway] khởi tạo giao dịch thủ công {} shopId={} amount={}đ",
                tx, request.getShopId(), request.getAmountVnd());

        String rawInit;
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("shopId", request.getShopId());
            meta.put("ownerId", request.getOwnerId());
            meta.put("amountVnd", request.getAmountVnd());
            rawInit = objectMapper.writeValueAsString(meta);
        } catch (Exception ex) {
            rawInit = "{}";
        }

        txnRepository.save(PaymentTransaction.builder()
                .shopId(request.getShopId())
                .ownerId(request.getOwnerId())
                .gateway(PaymentGatewayType.MANUAL)
                .providerTxnRef(tx)
                .amountVnd(request.getAmountVnd())
                .status(PaymentTransactionStatus.PENDING)
                .rawInitRequest(rawInit)
                .build());
        adminBillingService.ifAvailable(AdminBillingService::invalidateOverviewCache);

        return PaymentInitiation.builder()
                .gateway(PaymentGatewayType.MANUAL)
                .paymentUrl(null)
                .transactionId(tx)
                .amountVnd(request.getAmountVnd())
                .build();
    }
}
