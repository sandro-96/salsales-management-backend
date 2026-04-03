package com.example.sales.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportResponse {
    private LocalDate date;
    private long totalOrders;
    private long totalProductsSold;
    private double totalRevenue;
    private double totalAmount;
}
