// File: src/main/java/com/example/sales/dto/order/OrderItemResponse.java

package com.example.sales.dto.order;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {
    private String productId;
    private String productName;
    private int quantity;
    private double price;
    private double priceAfterDiscount; // giá sau khi đã áp dụng khuyến mãi
    private String appliedPromotionId; // id khuyến mãi được áp dụng (nếu có)
}
