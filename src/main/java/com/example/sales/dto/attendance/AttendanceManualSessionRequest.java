package com.example.sales.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Owner/Manager nhập hoặc sửa một ca chấm công (giờ vào / giờ ra) cho nhân viên.
 */
@Data
public class AttendanceManualSessionRequest {
    @NotBlank
    private String staffRef;
    /** SYSTEM | EXTERNAL (mặc định SYSTEM). */
    private String staffType;
    @NotNull
    private LocalDate workDate;
    @NotNull
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String note;
    /**
     * {@code true} (mặc định): ghi đè toàn bộ session của ngày đó bằng một ca mới.
     * {@code false}: thêm một ca nữa trong cùng ngày.
     */
    private Boolean replaceDay;
}
