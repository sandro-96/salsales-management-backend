// File: src/main/java/com/example/sales/dto/order/OrderItemRequest.java
package com.example.sales.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderItemRequest {
    @NotBlank
    private String productId;

    @Min(1)
    private int quantity;

    @Min(0)
    private double price;
}

