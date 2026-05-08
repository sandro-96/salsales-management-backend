package com.example.sales.dto.staffProfile;

import com.example.sales.constant.ContractType;
import com.example.sales.constant.ShopRole;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Tổng quan nhân sự cấp shop dùng cho trang Staff Dashboard.
 *
 * <p>Phase 1 chỉ trả các metric tổng quan (số lượng + chi phí lương ước tính).
 * Các block <i>attendance</i>, <i>leave</i>, <i>payroll</i> giữ schema rỗng/null
 * để UI không phải đổi khi phase 2–4 bật dữ liệu thật.
 */
@Data
@Builder
public class StaffShopOverviewResponse {

    /** Tháng thống kê dạng yyyy-MM (ví dụ "2026-05"). */
    private String month;

    /** Số nhân sự đang hoạt động (bao gồm cả OWNER + external). */
    private long totalStaff;
    /** Nhân sự có tài khoản hệ thống (ShopUser). */
    private long systemStaff;
    /** Nhân sự ngoài hệ thống (StaffProfile không gắn userId). */
    private long externalStaff;

    /** Số lượng theo từng vai trò (key = ShopRole.name(), value = số lượng). */
    private Map<ShopRole, Long> staffByRole;

    /** Số nhân sự theo từng chi nhánh (key = branchId, value = count). */
    private List<BranchStaffCount> staffByBranch;

    /** Tổng lương cố định theo tháng (cộng dồn salary đã khai báo trong StaffProfile). */
    private double totalMonthlySalary;
    /** Lương trung bình mỗi nhân sự (chỉ tính những người có salary > 0). */
    private double averageSalary;
    /** Số nhân sự đã có khai báo lương. */
    private long staffWithSalary;
    /** Phân bổ chi phí theo loại hợp đồng (FULL_TIME/PART_TIME/...) */
    private Map<ContractType, ContractTypeBreakdown> payrollByContract;

    /** Nhân sự mới gia nhập trong tháng đang xét (createdAt ∈ tháng). */
    private long newStaffThisMonth;

    /** Phase 2 placeholder — phục vụ chấm công. */
    private AttendanceSummary attendance;
    /** Phase 3 placeholder — phục vụ nghỉ phép. */
    private LeaveSummary leave;
    /** Phase 4 placeholder — payroll thực tế (sau khấu trừ/thưởng). */
    private PayrollSummary payroll;

    @Data
    @Builder
    public static class BranchStaffCount {
        private String branchId;
        private String branchName;
        private long count;
    }

    @Data
    @Builder
    public static class ContractTypeBreakdown {
        private long staffCount;
        private double totalSalary;
    }

    @Data
    @Builder
    public static class AttendanceSummary {
        @Builder.Default
        private boolean enabled = false;
        private Long totalShiftsScheduled;
        private Long totalShiftsCompleted;
        private Long totalLateArrivals;
        private Long totalEarlyLeaves;
        /** Tổng phút làm việc trong tháng (cộng các session đã check-out). */
        private Long totalWorkMinutes;
    }

    @Data
    @Builder
    public static class LeaveSummary {
        @Builder.Default
        private boolean enabled = false;
        private Long totalLeaveDays;
        private Long pendingRequests;
        private Long approvedRequests;
        private Long rejectedRequests;
    }

    @Data
    @Builder
    public static class PayrollSummary {
        @Builder.Default
        private boolean enabled = false;
        private Double grossPayroll;
        private Double netPayroll;
        private Double bonusTotal;
        private Double deductionTotal;
        private String status;
    }
}
