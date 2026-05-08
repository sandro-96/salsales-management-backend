package com.example.sales.dto.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AttendanceEntryResponse {
    private String id;
    private String shopId;
    private String staffRef;
    private String staffType;
    private String branchId;
    private LocalDate workDate;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String note;

    /** Danh sách session/ca trong ngày (Phase 2+). */
    private List<AttendanceSessionResponse> sessions;

    @Data
    @Builder
    public static class AttendanceSessionResponse {
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
        private String note;
        /** Tổng phút của session (chỉ tính khi có checkOutAt). */
        private Long minutes;
    }

    /** Tổng phút làm trong ngày (cộng các session đã check-out). */
    private Long totalMinutes;
}

