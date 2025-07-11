// File: src/main/java/com/example/sales/dto/report/ReportRequest.java
package com.example.sales.dto.report;

import com.example.sales.constant.OrderStatus;
import com.example.sales.validation.ValidDateRange;
import lombok.Data;

import java.time.LocalDate;

@Data
@ValidDateRange
public class ReportRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private OrderStatus status; // optional
}
