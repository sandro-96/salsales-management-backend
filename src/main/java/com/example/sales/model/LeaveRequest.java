package com.example.sales.model;

import com.example.sales.constant.LeaveRequestStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("leave_requests")
@CompoundIndex(name = "shop_staff_date_idx", def = "{'shopId': 1, 'staffRef': 1, 'fromDate': 1, 'toDate': 1}")
public class LeaveRequest extends BaseEntity {

    @Id
    private String id;

    private String shopId;

    /** userId hệ thống hoặc profileId external. */
    private String staffRef;
    /** SYSTEM | EXTERNAL */
    private String staffType;

    private String branchId;

    /** Loại nghỉ (VD: ANNUAL, SICK, UNPAID...). Phase 3 để string cho linh hoạt. */
    private String type;

    private LocalDate fromDate;
    private LocalDate toDate;

    private String reason;

    @Builder.Default
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    private String approvedBy;
    private LocalDateTime approvedAt;

    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectedReason;

    private String cancelledBy;
    private LocalDateTime cancelledAt;
}

