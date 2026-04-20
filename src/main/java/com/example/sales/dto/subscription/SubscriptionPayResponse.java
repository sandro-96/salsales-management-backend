// File: src/main/java/com/example/sales/dto/subscription/SubscriptionPayResponse.java
package com.example.sales.dto.subscription;

import com.example.sales.constant.PaymentGatewayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPayResponse {
    private PaymentGatewayType gateway;
    private String paymentUrl;
    private String transactionId;
    private long amountVnd;
}
