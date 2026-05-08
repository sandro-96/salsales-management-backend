package com.example.sales.dto.payroll;

import com.example.sales.constant.PayrollRunStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PayrollRunResponse {
    private String id;
    private String shopId;
    /** yyyy-MM */
    private String month;
    private PayrollRunStatus status;

    private Double grossTotal;
    private Double netTotal;
    private Double bonusTotal;
    private Double deductionTotal;

    private String finalizedBy;
    private LocalDateTime finalizedAt;
    private String paidBy;
    private LocalDateTime paidAt;

    private List<PayrollItemResponse> items;
}

