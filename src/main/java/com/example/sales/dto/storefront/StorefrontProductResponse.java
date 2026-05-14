// File: src/main/java/com/example/sales/dto/storefront/StorefrontProductResponse.java
package com.example.sales.dto.storefront;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Thông tin sản phẩm hiển thị trên storefront công khai.
 * Bỏ qua các trường nội bộ (cost price, supplierId, trackInventory,...).
 */
@Data
@Builder
public class StorefrontProductResponse {
    private String id;
    private String name;
    private String category;
    private String description;
    private String unit;
    private double price;
    private List<String> images;
    private List<StorefrontProductVariantResponse> variants;
}
