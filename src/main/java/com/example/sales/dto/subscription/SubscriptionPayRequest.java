// File: src/main/java/com/example/sales/dto/subscription/SubscriptionPayRequest.java
package com.example.sales.dto.subscription;

import com.example.sales.constant.PaymentGatewayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request khởi tạo thanh toán gia hạn subscription.
 * {@code gateway} có thể null — service sẽ dùng gateway mặc định (config).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPayRequest {
    private PaymentGatewayType gateway;
    /** Số tháng gia hạn: 1, 3, 6, 9 hoặc 12. Mặc định 1. */
    private Integer billingMonths;
    /** URL client muốn trở về sau khi thanh toán. */
    private String returnUrl;
}
