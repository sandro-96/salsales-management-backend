// File: src/main/java/com/example/sales/dto/storefront/TableOrderRequest.java
package com.example.sales.dto.storefront;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Payload guest dùng để gửi đơn từ trang QR self-ordering tại bàn.
 * Khác với {@link StorefrontOrderRequest}: không yêu cầu địa chỉ giao hàng / phone — chỉ items
 * và ghi chú tuỳ chọn (vd "không cay", "thêm đá"). Tên & số bàn lấy từ qrToken trên URL.
 */
@Data
public class TableOrderRequest {

    /** Ghi chú tuỳ chọn cho toàn đơn (vd: "không hành"). */
    @Size(max = 500)
    private String customerNote;

    /** Tên khách tuỳ chọn (vd nhóm khách muốn ghi "Đoàn anh Hùng"). */
    @Size(max = 120)
    private String customerName;

    @Valid
    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    private List<StorefrontOrderItemRequest> items;
}
