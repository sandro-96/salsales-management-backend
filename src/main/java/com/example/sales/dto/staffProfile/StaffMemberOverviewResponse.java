package com.example.sales.dto.staffProfile;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Tổng quan của 1 nhân sự cụ thể (trang Staff Detail).
 *
 * <p>Phase 1: trả về profile + skeleton attendance/leave/payroll để UI có thể
 * render placeholder card. Khi phase 2–4 release, các trường này sẽ được điền
 * mà không phải đổi schema phía FE.
 */
@Data
@Builder
public class StaffMemberOverviewResponse {

    private StaffProfileResponse profile;

    private String month;

    private AttendanceBlock attendance;
    private LeaveBlock leave;
    private PayrollBlock payroll;

    @Data
    @Builder
    public static class AttendanceBlock {
        @Builder.Default
        private boolean enabled = false;
        private Long totalShifts;
        private Long completedShifts;
        private Long lateArrivals;
        private Long earlyLeaves;
        private List<DailyAttendanceEntry> recentEntries;
    }

    @Data
    @Builder
    public static class DailyAttendanceEntry {
        private String date;
        private String checkInAt;
        private String checkOutAt;
        private String status;
    }

    @Data
    @Builder
    public static class LeaveBlock {
        @Builder.Default
        private boolean enabled = false;
        private Long totalLeaveDays;
        private Long approvedDays;
        private Long pendingDays;
        private List<LeaveRequestEntry> recentRequests;
    }

    @Data
    @Builder
    public static class LeaveRequestEntry {
        private String id;
        private String type;
        private String fromDate;
        private String toDate;
        private String status;
    }

    @Data
    @Builder
    public static class PayrollBlock {
        @Builder.Default
        private boolean enabled = false;
        private Double baseSalary;
        private Double grossSalary;
        private Double netSalary;
        private Double bonus;
        private Double deduction;
        private List<PayrollHistoryEntry> recentRuns;
    }

    @Data
    @Builder
    public static class PayrollHistoryEntry {
        private String month;
        private Double grossSalary;
        private Double netSalary;
        private String status;
    }
}
