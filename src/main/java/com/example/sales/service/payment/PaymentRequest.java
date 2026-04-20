// File: src/main/java/com/example/sales/service/payment/PaymentRequest.java
package com.example.sales.service.payment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentRequest {
    private String shopId;
    private String ownerId;
    private long amountVnd;
    private String description;
    /** URL client trở về sau khi thanh toán (stub có thể ignore). */
    private String returnUrl;
}
