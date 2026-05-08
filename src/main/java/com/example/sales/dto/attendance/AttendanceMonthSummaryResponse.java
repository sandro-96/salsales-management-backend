package com.example.sales.dto.attendance;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttendanceMonthSummaryResponse {
    private String shopId;
    /** yyyy-MM */
    private String month;
    private String staffRef;
    private String staffType;
    private long totalDays;
    private long checkedInDays;
    private long checkedOutDays;
    private List<AttendanceEntryResponse> entries;
}

