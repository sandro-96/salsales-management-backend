package com.example.sales.model;

import com.example.sales.constant.PayrollRunStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("payroll_runs")
@CompoundIndex(name = "shop_month_idx", def = "{'shopId': 1, 'month': 1}")
public class PayrollRun extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    /** yyyy-MM */
    private String month;

    @Builder.Default
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    private Double grossTotal;
    private Double netTotal;
    private Double bonusTotal;
    private Double deductionTotal;

    private String finalizedBy;
    private LocalDateTime finalizedAt;

    private String paidBy;
    private LocalDateTime paidAt;
}

