// File: src/main/java/com/example/sales/model/OrderItem.java
package com.example.sales.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String productId;       // ID của Product (master product)
    private String branchProductId; // ID của BranchProduct (sản phẩm cụ thể tại chi nhánh)
    /** null = sản phẩm không có biến thể / dòng đơn cũ */
    private String variantId;
    private String productName;
    /** Tên biến thể tại thời điểm đặt (snapshot, có thể null) */
    private String variantName;
    /** SKU dòng hàng (master hoặc biến thể) */
    private String sku;
    /** Tên CTKM đã áp dụng (snapshot) */
    private String promotionName;
    /** Mô tả nhanh mức giảm, ví dụ 10% hoặc 5.000 ₫ */
    private String promotionDiscountLabel;
    private int quantity;
    private double price;             // Giá gốc tại thời điểm đặt hàng
    private double priceAfterDiscount; // Giá sau khi áp dụng khuyến mãi
    private String appliedPromotionId; // ID của khuyến mãi đã áp dụng
    private boolean trackInventory; // Có theo dõi tồn kho không

    /** Topping đã chọn (snapshot); null hoặc rỗng = không có */
    private List<OrderLineTopping> toppings;
}