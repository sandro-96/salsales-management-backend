// File: src/main/java/com/example/sales/dto/storefront/TableOrderResponse.java
package com.example.sales.dto.storefront;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Đơn hiện tại của bàn — guest có thể GET để xem món đã order, tổng tiền, trạng thái.
 * Không expose customer info chi tiết (privacy giữa các lượt khách dùng cùng QR).
 */
@Data
@Builder
public class TableOrderResponse {

    private String id;
    private String orderCode;
    private String status;
    private String paymentMethod;
    private boolean paid;
    private double totalPrice;
    private double totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String customerNote;
    private List<TableOrderLine> items;

    @Data
    @Builder
    public static class TableOrderLine {
        private String productId;
        private String productName;
        private String variantName;
        private int quantity;
        private double unitPrice;
        private double lineTotal;
    }
}
