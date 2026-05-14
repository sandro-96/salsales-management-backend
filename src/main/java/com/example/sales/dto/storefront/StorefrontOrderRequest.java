// File: src/main/java/com/example/sales/dto/storefront/StorefrontOrderRequest.java
package com.example.sales.dto.storefront;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Payload guest checkout từ storefront. Không yêu cầu auth.
 * Địa chỉ giao hàng sẽ được service ghép vào {@code Order.note} để không thay đổi schema cũ.
 */
@Data
public class StorefrontOrderRequest {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 120)
    private String customerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(max = 30)
    private String customerPhone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 200)
    private String customerEmail;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    @Size(max = 300)
    private String addressLine;

    @Size(max = 100)
    private String ward;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String province;

    @Size(max = 500)
    private String note;

    @Valid
    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    private List<StorefrontOrderItemRequest> items;
}
