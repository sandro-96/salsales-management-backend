package com.example.sales.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProductCatalogBulkImportRequest {

    @NotEmpty
    @Size(max = 100)
    @Valid
    private List<ProductCatalogUpsertRequest> items;
}
