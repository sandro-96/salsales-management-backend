// File: src/main/java/com/example/sales/model/OrderItem.java
package com.example.sales.model;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String productId;       // ID của Product (master product)
    private String branchProductId; // ID của BranchProduct (sản phẩm cụ thể tại chi nhánh)
    private String productName;
    private int quantity;
    private double price;             // Giá gốc tại thời điểm đặt hàng
    private double priceAfterDiscount; // Giá sau khi áp dụng khuyến mãi
    private String appliedPromotionId; // ID của khuyến mãi đã áp dụng
}