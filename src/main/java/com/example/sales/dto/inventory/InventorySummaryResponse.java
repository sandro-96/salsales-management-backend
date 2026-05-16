package com.example.sales.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryResponse {
    private long trackedProducts;
    private long totalStock;
    private long lowStock;
    private long outOfStock;
    private long notTracked;
}
