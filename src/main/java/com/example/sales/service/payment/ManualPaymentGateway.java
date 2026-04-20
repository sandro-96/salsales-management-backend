// File: src/main/java/com/example/sales/service/payment/ManualPaymentGateway.java
package com.example.sales.service.payment;

import com.example.sales.constant.PaymentGatewayType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Gateway mặc định — dùng cho trường hợp admin/shop chuyển khoản thủ công.
 * Không tạo URL redirect thực; trả về transactionId tạm để admin có thể gắn vào khi
 * xác nhận thanh toán thủ công qua UI admin.
 */
@Slf4j
@Component
public class ManualPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayType type() {
        return PaymentGatewayType.MANUAL;
    }

    @Override
    public PaymentInitiation initiatePayment(PaymentRequest request) {
        String tx = "MANUAL-" + UUID.randomUUID();
        log.info("[ManualPaymentGateway] khởi tạo giao dịch thủ công {} shopId={} amount={}đ",
                tx, request.getShopId(), request.getAmountVnd());
        return PaymentInitiation.builder()
                .gateway(PaymentGatewayType.MANUAL)
                .paymentUrl(null)
                .transactionId(tx)
                .amountVnd(request.getAmountVnd())
                .build();
    }
}
