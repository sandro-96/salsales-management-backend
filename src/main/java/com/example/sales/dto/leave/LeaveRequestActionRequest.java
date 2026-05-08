package com.example.sales.dto.leave;

import lombok.Data;

@Data
public class LeaveRequestActionRequest {
    /** Lý do từ chối / huỷ (optional). */
    private String reason;
}

