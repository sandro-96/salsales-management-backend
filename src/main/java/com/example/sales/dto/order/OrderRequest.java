// File: src/main/java/com/example/sales/dto/order/OrderRequest.java
package com.example.sales.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotBlank(message = "Shop ID không được để trống")
    private String shopId;

    @NotBlank(message = "Branch ID không được để trống")
    private String branchId; // Đơn hàng luôn thuộc về một chi nhánh cụ thể

    private String tableId; // Có thể null nếu không phải đơn hàng tại bàn

    private String note;

    @Valid
    @NotEmpty(message = "Đơn hàng phải có ít nhất một sản phẩm")
    private List<OrderItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotBlank(message = "Product ID không được để trống")
        private String productId; // Đây là ID của Product master

        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private int quantity;

        @Min(value = 0, message = "Giá sản phẩm không được âm")
        private double price; // Giá tại thời điểm đặt hàng (có thể khác giá hiện tại của sản phẩm)
    }
}