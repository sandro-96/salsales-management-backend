// File: src/main/java/com/example/sales/dto/order/OrderRequest.java
package com.example.sales.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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

    /** Tên khách trên đơn (ghi nhận) — khách hàng loyalty. */
    private String guestName;
    /** SĐT khách trên đơn (ghi nhận). */
    private String guestPhone;

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

        /**
         * Số lượng đơn vị (sản phẩm thường). Khi sản phẩm bán theo cân ({@code sellByWeight}),
         * server sẽ đặt = 1 và sử dụng {@link #weight}.
         */
        @Min(value = 0, message = "Số lượng không được âm")
        private int quantity;

        /**
         * Trọng lượng/thể tích thực cho sản phẩm bán theo cân (VD 0.5 khi unit=kg = 500g).
         * Bắt buộc > 0 khi {@code Product.sellByWeight = true}, null cho sản phẩm thường.
         */
        @DecimalMin(value = "0.0", inclusive = false, message = "Trọng lượng phải lớn hơn 0")
        private Double weight;

        /**
         * ID topping shop đã chọn (trùng {@code ShopTopping#toppingId}), thứ tự không quan trọng — server chuẩn hoá.
         */
        private List<String> toppingIds;
    }
}