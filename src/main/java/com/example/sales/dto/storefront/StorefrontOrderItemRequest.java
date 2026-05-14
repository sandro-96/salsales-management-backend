// File: src/main/java/com/example/sales/dto/storefront/StorefrontOrderItemRequest.java
package com.example.sales.dto.storefront;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorefrontOrderItemRequest {

    @NotBlank(message = "productId không được để trống")
    private String productId;

    /** Tuỳ chọn — bắt buộc khi product có variants. */
    private String variantId;

    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private int quantity;
}
