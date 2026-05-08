package com.example.sales.dto.payroll;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayrollItemResponse {
    private String id;
    private String runId;
    private String shopId;
    /** yyyy-MM */
    private String month;
    private String staffRef;
    private String staffType;
    private String branchId;

    private Double baseSalary;
    private Double grossSalary;
    private Double bonus;
    private Double deduction;
    private Double netSalary;

    private Long workMinutes;
    private Long approvedLeaveDays;
    private Long unpaidLeaveDays;
}

