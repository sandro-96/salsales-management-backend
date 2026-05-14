// File: src/main/java/com/example/sales/dto/storefront/StorefrontOrderResponse.java
package com.example.sales.dto.storefront;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorefrontOrderResponse {
    private String id;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private double totalAmount;
    private String paymentMethod;
    private String status;
}
