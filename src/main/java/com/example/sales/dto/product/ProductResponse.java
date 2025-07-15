// File: src/main/java/com/example/sales/dto/product/ProductResponse.java
package com.example.sales.dto.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String category;
    private int quantity;
    private double price;
    private String unit;
    private String imageUrl;
    private String description;
    private boolean active;
    private String productCode;
    private String shopId;
    private String createdAt;
    private String updatedAt;
}
