package com.example.sales.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogOffBrowsePageResponse {

    private List<ProductCatalogOffBrowseItem> items;
    private int page;
    private int pageSize;
    /** Tổng ước lượng từ OFF (có thể thay đổi theo bộ lọc). */
    private long totalCount;
    private String source;
    private String countryTag;
}
