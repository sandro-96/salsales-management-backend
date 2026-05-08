package com.example.sales.dto.leave;

import com.example.sales.constant.LeaveRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LeaveRequestResponse {
    private String id;
    private String shopId;
    private String staffRef;
    private String staffType;
    private String branchId;
    private String type;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private LeaveRequestStatus status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectedReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

