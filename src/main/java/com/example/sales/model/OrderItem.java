// File: src/main/java/com/example/sales/model/OrderItem.java
package com.example.sales.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String productId;
    private String productName;
    private int quantity;
    private double price;
    private double priceAfterDiscount;
    private String appliedPromotionId;
}
