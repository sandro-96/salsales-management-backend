// File: src/main/java/com/example/sales/dto/product/ProductResponse.java
package com.example.sales.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO dùng để trả về thông tin chi tiết của một sản phẩm tại một chi nhánh cụ thể.
 * Kết hợp thông tin từ cả Product (định nghĩa chung) và BranchProduct (chi tiết chi nhánh).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    // ID của bản ghi BranchProduct
    private String id;

    // ID của định nghĩa sản phẩm chung (từ Product)
    private String productId;

    // Các trường từ Product (định nghĩa chung)
    private String name;
    private String category;
    private String sku;

    // Các trường từ BranchProduct (chi tiết tại chi nhánh)
    private int quantity;
    private double price;
    private String unit;
    private String imageUrl;
    private String description;
    private String branchId;
    private boolean activeInBranch; // Trạng thái kích hoạt tại chi nhánh này

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}