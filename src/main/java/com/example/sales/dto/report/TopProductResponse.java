package com.example.sales.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {
    private String productId;
    private String productName;
    private long totalQuantitySold;
    private double totalRevenue;
}
