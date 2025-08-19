// File: src/main/java/com/example/sales/dto/product/ProductResponse.java
package com.example.sales.dto.product;

import com.example.sales.model.BranchProductVariant;
import com.example.sales.model.PriceHistory;
import com.example.sales.model.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private Map<String, String> nameTranslations;
    private String category;
    private String sku;
    private double costPrice;
    private double defaultPrice;
    private String unit;
    private String description;
    private List<String> images;
    private boolean active;
    private String barcode;
    private String supplierId;
    private List<ProductVariant> variants;
    private List<PriceHistory> priceHistory;

    // Các trường từ BranchProduct (chi tiết tại chi nhánh)
    private int quantity;
    private int minQuantity;
    private double price;
    private double branchCostPrice;
    private Double discountPrice;
    private Double discountPercentage;
    private LocalDate expiryDate;
    private List<BranchProductVariant> branchVariants;
    private String branchId;
    private boolean activeInBranch; // Trạng thái kích hoạt tại chi nhánh này

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}