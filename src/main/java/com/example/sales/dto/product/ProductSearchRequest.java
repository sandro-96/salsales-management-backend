// File: ProductSearchRequest.java
package com.example.sales.dto.product;

import lombok.Data;

@Data
public class ProductSearchRequest {
    private String keyword = "";
    private String category = "";
    private Boolean active;
    private Double minPrice;
    private Double maxPrice;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
    private String branchId; // nếu có chi nhánh, có thể để null nếu không có
}
