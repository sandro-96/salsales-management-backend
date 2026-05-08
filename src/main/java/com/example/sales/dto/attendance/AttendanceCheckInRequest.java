package com.example.sales.dto.attendance;

import lombok.Data;

/**
 * Phase 2: chấm công tối thiểu.
 *
 * <p>Nếu {@code staffRef} trống → check-in cho chính user đang đăng nhập.
 * Nếu có staffRef + staffType=EXTERNAL → manager/owner check-in cho nhân sự ngoài hệ thống.
 */
@Data
public class AttendanceCheckInRequest {
    private String staffRef;
    /** SYSTEM | EXTERNAL (mặc định SYSTEM). */
    private String staffType;
    private String branchId;
    private String note;
}

