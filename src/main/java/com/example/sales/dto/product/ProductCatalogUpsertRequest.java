package com.example.sales.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Tạo / cập nhật một bản ghi {@link com.example.sales.model.ProductCatalog} (chỉ qua system admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogUpsertRequest {

    @NotBlank
    private String barcode;

    @NotBlank
    private String name;

    private String category;

    private String description;

    private List<String> images;
}
