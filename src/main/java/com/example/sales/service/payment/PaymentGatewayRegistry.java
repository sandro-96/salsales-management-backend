// File: src/main/java/com/example/sales/service/payment/PaymentGatewayRegistry.java
package com.example.sales.service.payment;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolver chọn {@link PaymentGateway} theo config {@code app.payment.gateway} (default MANUAL).
 * <p>
 * Có thể override theo request (VD admin test) bằng cách truyền explicit type vào
 * {@link #resolve(PaymentGatewayType)}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayRegistry {

    private final List<PaymentGateway> gateways;
    private final Map<PaymentGatewayType, PaymentGateway> byType = new EnumMap<>(PaymentGatewayType.class);

    @Value("${app.payment.gateway:MANUAL}")
    private String defaultGatewayName;

    @PostConstruct
    void init() {
        for (PaymentGateway g : gateways) {
            byType.put(g.type(), g);
        }
        log.info("[PaymentGatewayRegistry] đăng ký {} gateway: {}", byType.size(), byType.keySet());
    }

    public PaymentGatewayType defaultGateway() {
        try {
            return PaymentGatewayType.valueOf(defaultGatewayName.trim().toUpperCase());
        } catch (Exception ex) {
            log.warn("[PaymentGatewayRegistry] config app.payment.gateway='{}' không hợp lệ, fallback MANUAL",
                    defaultGatewayName);
            return PaymentGatewayType.MANUAL;
        }
    }

    public PaymentGateway resolve(PaymentGatewayType type) {
        PaymentGatewayType t = type != null ? type : defaultGateway();
        PaymentGateway gw = byType.get(t);
        if (gw == null) {
            throw new BusinessException(ApiCode.PAYMENT_GATEWAY_ERROR);
        }
        return gw;
    }
}
