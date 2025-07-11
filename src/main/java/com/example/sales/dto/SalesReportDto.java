// File: src/main/java/com/example/sales/dto/SalesReportDto.java
package com.example.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SalesReportDto {
    private LocalDate date;
    private long totalOrders;
    private double totalAmount;
}
