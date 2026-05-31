package com.example.sales.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Một dòng gợi ý từ Open Food Facts (chưa lưu). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogOffBrowseItem {

    private String barcode;
    private String name;
    private String category;
    private String description;
    private List<String> images;
    /** Đã có trong catalog nội bộ (theo barcode). */
    private boolean alreadyInCatalog;
}
