package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("payroll_items")
@CompoundIndex(name = "run_staff_idx", def = "{'runId': 1, 'staffRef': 1}")
public class PayrollItem extends BaseEntity {

    @Id
    private String id;

    private String runId;
    private String shopId;
    /** yyyy-MM */
    private String month;

    private String staffRef;
    /** SYSTEM | EXTERNAL */
    private String staffType;

    private String branchId;

    private Double baseSalary;
    private Double grossSalary;
    private Double bonus;
    private Double deduction;
    private Double netSalary;

    /** Tổng phút làm việc trong tháng (từ attendance logs). */
    private Long workMinutes;
    /** Tổng ngày nghỉ phép đã duyệt trong tháng. */
    private Long approvedLeaveDays;
    /** Tổng ngày nghỉ không lương (UNPAID) đã duyệt trong tháng. */
    private Long unpaidLeaveDays;
}

