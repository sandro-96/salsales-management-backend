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
    /** URL client muốn trở về sau khi thanh toán. */
    private String returnUrl;
}
