package com.example.sales.dto.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AttendanceDaySummaryResponse {
    private String shopId;
    private LocalDate workDate;
    private long totalEntries;
    private long checkedInCount;
    private long checkedOutCount;
    private List<AttendanceEntryResponse> entries;
}

