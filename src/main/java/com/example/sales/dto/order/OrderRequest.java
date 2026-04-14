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

    private String customerId;          // Khách hàng (tuỳ chọn, để tích điểm)

    /** Tuỳ chọn — ghi nhận khi tạo đơn bán online / ship ngoài */
    private String shippingCarrier;
    private String shippingMethod;
    private String trackingNumber;
    private String externalOrderRef;

    /**
     * Khi tạo đơn kèm hình thức thu tiền sau (vd {@code ShipCOD}): đặt phương thức dự kiến,
     * {@code paymentStatus = PENDING_COLLECTION}, chưa thanh toán, trạng thái đơn {@code CONFIRMED}.
     */
    private String checkoutPaymentMethod;

    @Min(value = 0, message = "Số điểm đổi phải >= 0")
    private long pointsToRedeem;        // Số điểm muốn đổi (0 = không đổi)

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

        /**
         * Bắt buộc khi sản phẩm có biến thể — trùng {@code ProductVariant.variantId} /
         * {@code BranchProductVariant.variantId}.
         */
        private String variantId;

        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private int quantity;
    }
}