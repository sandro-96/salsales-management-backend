// File: src/main/java/com/example/sales/dto/product/ProductRequest.java
package com.example.sales.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để tạo hoặc cập nhật thông tin sản phẩm tại một chi nhánh cụ thể.
 * Các trường như name, category, sku sẽ ảnh hưởng đến định nghĩa sản phẩm chung (Product).
 * Các trường như price, quantity, unit, imageUrl, description, active sẽ ảnh hưởng đến BranchProduct.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    // Các trường ảnh hưởng đến Product (định nghĩa chung)
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    @NotBlank(message = "SKU không được để trống")
    private String sku; // SKU là duy nhất trong phạm vi shop, dùng để tìm/tạo Product chính

    // Các trường ảnh hưởng đến BranchProduct (chi tiết tại chi nhánh)
    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private double price;

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit;

    private String imageUrl;
    private String description;

    // active sẽ được ánh xạ tới activeInBranch của BranchProduct
    @Builder.Default
    private boolean active = true;
}