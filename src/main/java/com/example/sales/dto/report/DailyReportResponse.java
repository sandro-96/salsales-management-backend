// File: src/main/java/com/example/sales/dto/report/DailyReportResponse.java
package com.example.sales.dto.report;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyReportResponse {
    private LocalDate date;
    private long totalOrders;
    private long totalProductsSold;
    private double totalRevenue;
}
