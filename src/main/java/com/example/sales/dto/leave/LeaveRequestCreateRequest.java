package com.example.sales.dto.leave;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestCreateRequest {
    private String staffRef;
    /** SYSTEM | EXTERNAL (mặc định SYSTEM). */
    private String staffType;
    private String branchId;
    private String type;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
}

