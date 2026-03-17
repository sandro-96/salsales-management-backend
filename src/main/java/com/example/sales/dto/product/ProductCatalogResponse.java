package com.example.sales.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin gợi ý từ Internal Catalog khi tra cứu barcode.
 * Frontend dùng để pre-fill form tạo sản phẩm mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogResponse {
    private String id;
    private String barcode;
    private String name;
    private String category;
    private String description;
    private List<String> images;
    /** Thời điểm barcode này được đưa vào catalog lần đầu */
    private LocalDateTime createdAt;
    /** Thời điểm thông tin catalog được cập nhật lần cuối */
    private LocalDateTime updatedAt;
}

