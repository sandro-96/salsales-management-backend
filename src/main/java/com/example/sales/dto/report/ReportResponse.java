// File: src/main/java/com/example/sales/dto/report/ReportResponse.java
package com.example.sales.dto.report;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportResponse {
    private long totalOrders;
    private long totalProductsSold;
    private double totalRevenue;
}
