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
public class ProductCatalogBulkImportResponse {

    private int imported;
    private int failed;
    private List<String> errors;
}
