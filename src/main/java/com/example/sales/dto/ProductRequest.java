// File: ProductRequest.java
package com.example.sales.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private double price;

    private String unit;

    private String imageUrl;

    private String description;

    private boolean active = true;
}
