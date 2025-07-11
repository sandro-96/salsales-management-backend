// File: src/main/java/com/example/sales/dto/product/ProductRequest.java

package com.example.sales.dto.product;

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
    private String productCode;
    private String branchId; // nếu có chi nhánh, có thể để null nếu không có
}
