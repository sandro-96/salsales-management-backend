// File: src/main/java/com/example/sales/dto/order/OrderUpdateRequest.java
package com.example.sales.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequest {
    private String tableId;
    private String note;

    @Valid
    private List<OrderItemUpdateRequest> items; // Có thể cập nhật danh sách sản phẩm

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemUpdateRequest {
        // ID của Product master, không phải BranchProduct ID
        private String productId;

        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private int quantity;

        @Min(value = 0, message = "Giá sản phẩm không được âm")
        private double price; // Giá tại thời điểm cập nhật (có thể khác giá hiện tại của sản phẩm)
    }
}