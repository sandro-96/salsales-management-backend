// File: src/main/java/com/example/sales/dto/order/OrderItemResponse.java
package com.example.sales.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private String productId;       // ID của Product (master product)
    private String branchProductId; // ID của BranchProduct (sản phẩm cụ thể tại chi nhánh)
    private String variantId;
    private String productName;
    private String variantName;
    /** Thuộc tính biến thể (chuỗi ghép từ catalog khi xem đơn), có thể null */
    private String variantAttributesText;
    private String sku;
    private String promotionName;
    private String promotionDiscountLabel;
    private int quantity;
    private double price;             // Giá gốc tại thời điểm đặt hàng
    private double priceAfterDiscount; // Giá sau khi áp dụng khuyến mãi
    private String appliedPromotionId; // ID của khuyến mãi đã áp dụng

    /** true nếu dòng hàng bán theo cân (subtotal = price × weight). */
    private boolean sellByWeight;
    /** Trọng lượng/thể tích khi {@link #sellByWeight} = true (VD 0.5 cho 500g khi unit=kg). */
    private Double weight;
    /** Đơn vị trọng lượng (kg, g, l, ml) - snapshot từ product.unit. */
    private String weightUnit;

    private List<OrderLineToppingResponse> toppings;
}