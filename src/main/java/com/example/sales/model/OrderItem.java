// File: src/main/java/com/example/sales/model/OrderItem.java
package com.example.sales.model;

import lombok.Data;

@Data
public class OrderItem {
    private String productId;
    private String productName;
    private int quantity;
    private double price;
    private double priceAfterDiscount; // giá sau khi đã áp dụng khuyến mãi
    private String appliedPromotionId; // id khuyến mãi được áp dụng (nếu có)
}

