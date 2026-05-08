package com.example.sales.dto.attendance;

import lombok.Data;

@Data
public class AttendanceCheckOutRequest {
    private String staffRef;
    /** SYSTEM | EXTERNAL (mặc định SYSTEM). */
    private String staffType;
    private String note;
}

