package com.example.sales.service;

import com.example.sales.constant.*;
import com.example.sales.dto.payroll.PayrollItemResponse;
import com.example.sales.dto.payroll.PayrollRunResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.*;
import com.example.sales.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ShopUserRepository shopUserRepository;
    private final StaffProfileRepository staffProfileRepository;

    public PayrollRunResponse generateOrRecompute(String shopId, String monthOpt, String actorUserId) {
        YearMonth ym = parseMonthOrNow(monthOpt);
        String month = ym.toString();

        PayrollRun run = payrollRunRepository.findByShopIdAndMonthAndDeletedFalse(shopId, month)
                .orElseGet(() -> PayrollRun.builder().shopId(shopId).month(month).build());
        if (run.getStatus() == PayrollRunStatus.FINALIZED || run.getStatus() == PayrollRunStatus.PAID) {
            throw new BusinessException(ApiCode.PAYROLL_ALREADY_FINALIZED);
        }

        // Persist run first so we always have runId; then replace items for this run only.
        run = payrollRunRepository.save(run);
        String runId = run.getId();
        List<PayrollItem> previousItems = payrollItemRepository.findByRunIdAndDeletedFalse(runId);
        if (!previousItems.isEmpty()) {
            payrollItemRepository.deleteAll(previousItems);
        }

        List<PayrollItem> items = computeItems(shopId, ym, runId);
        for (PayrollItem it : items) {
            it.setRunId(runId);
            payrollItemRepository.save(it);
        }

        List<PayrollItem> savedItems = payrollItemRepository.findByRunIdAndDeletedFalse(runId);
        double grossTotal = savedItems.stream().mapToDouble(i -> safe(i.getGrossSalary())).sum();
        double bonusTotal = savedItems.stream().mapToDouble(i -> safe(i.getBonus())).sum();
        double deductionTotal = savedItems.stream().mapToDouble(i -> safe(i.getDeduction())).sum();
        double netTotal = savedItems.stream().mapToDouble(i -> safe(i.getNetSalary())).sum();

        run.setGrossTotal(grossTotal);
        run.setNetTotal(netTotal);
        run.setBonusTotal(bonusTotal);
        run.setDeductionTotal(deductionTotal);
        run = payrollRunRepository.save(run);

        return toRunResponse(run, savedItems);
    }

    public PayrollRunResponse getRun(String shopId, String monthOpt) {
        YearMonth ym = parseMonthOrNow(monthOpt);
        String month = ym.toString();
        PayrollRun run = payrollRunRepository.findByShopIdAndMonthAndDeletedFalse(shopId, month)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PAYROLL_RUN_NOT_FOUND));
        List<PayrollItem> items = payrollItemRepository.findByRunIdAndDeletedFalse(run.getId());
        return toRunResponse(run, items);
    }

    public PayrollItemResponse getStaffMonthItem(String shopId, String monthOpt, String staffRef) {
        YearMonth ym = parseMonthOrNow(monthOpt);
        String month = ym.toString();
        PayrollItem item = payrollItemRepository
                .findFirstByShopIdAndMonthAndStaffRefAndDeletedFalseOrderByCreatedAtDesc(shopId, month, staffRef)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PAYROLL_ITEM_NOT_FOUND));
        return toItemResponse(item);
    }

    public PayrollRunResponse finalizeRun(String shopId, String monthOpt, String actorUserId) {
        YearMonth ym = parseMonthOrNow(monthOpt);
        String month = ym.toString();
        PayrollRun run = payrollRunRepository.findByShopIdAndMonthAndDeletedFalse(shopId, month)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PAYROLL_RUN_NOT_FOUND));
        if (run.getStatus() == PayrollRunStatus.FINALIZED || run.getStatus() == PayrollRunStatus.PAID) {
            return toRunResponse(run, payrollItemRepository.findByRunIdAndDeletedFalse(run.getId()));
        }
        run.setStatus(PayrollRunStatus.FINALIZED);
        run.setFinalizedBy(actorUserId);
        run.setFinalizedAt(LocalDateTime.now());
        run = payrollRunRepository.save(run);
        return toRunResponse(run, payrollItemRepository.findByRunIdAndDeletedFalse(run.getId()));
    }

    private List<PayrollItem> computeItems(String shopId, YearMonth ym, String runIdNullable) {
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        int daysInMonth = ym.lengthOfMonth();

        // Build staff list: system users + external profiles
        List<ShopUser> shopUsers = shopUserRepository
                .findByShopIdAndDeletedFalse(shopId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        List<StaffProfile> profiles = staffProfileRepository.findByShopIdAndDeletedFalse(shopId);
        List<StaffProfile> externalProfiles = profiles.stream().filter(p -> p.getUserId() == null).toList();

        List<PayrollItem> out = new ArrayList<>();

        for (ShopUser su : shopUsers) {
            if (su.getUserId() == null) continue;
            StaffProfile p = profiles.stream().filter(x -> su.getUserId().equals(x.getUserId())).findFirst().orElse(null);
            out.add(computeOne(shopId, ym.toString(), runIdNullable, su.getUserId(), "SYSTEM",
                    p != null ? p.getBranchId() : null,
                    p != null ? p.getSalary() : null,
                    from, to, daysInMonth));
        }
        for (StaffProfile ep : externalProfiles) {
            if (ep.getId() == null) continue;
            out.add(computeOne(shopId, ym.toString(), runIdNullable, ep.getId(), "EXTERNAL",
                    ep.getBranchId(), ep.getSalary(), from, to, daysInMonth));
        }
        return out;
    }

    private PayrollItem computeOne(
            String shopId,
            String month,
            String runIdNullable,
            String staffRef,
            String staffType,
            String branchId,
            Double salary,
            LocalDate from,
            LocalDate to,
            int daysInMonth
    ) {
        double base = salary != null ? salary : 0.0;

        // Attendance minutes
        List<AttendanceLog> att = attendanceLogRepository
                .findByShopIdAndStaffRefAndWorkDateBetweenAndDeletedFalse(shopId, staffRef, from, to);
        long workMinutes = 0L;
        for (AttendanceLog log : att) {
            if (log.getSessions() != null && !log.getSessions().isEmpty()) {
                for (AttendanceLog.AttendanceSession s : log.getSessions()) {
                    if (s.getCheckInAt() != null && s.getCheckOutAt() != null) {
                        long m = ChronoUnit.MINUTES.between(s.getCheckInAt(), s.getCheckOutAt());
                        if (m > 0) workMinutes += m;
                    }
                }
            } else if (log.getCheckInAt() != null && log.getCheckOutAt() != null) {
                long m = ChronoUnit.MINUTES.between(log.getCheckInAt(), log.getCheckOutAt());
                if (m > 0) workMinutes += m;
            }
        }

        // Leave days
        List<LeaveRequest> leaves = leaveRequestRepository
                .findByShopIdAndStaffRefAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDeletedFalse(
                        shopId, staffRef, from, to);
        long approvedDays = 0L;
        long unpaidDays = 0L;
        for (LeaveRequest lr : leaves) {
            if (lr.getStatus() != LeaveRequestStatus.APPROVED) continue;
            long d = ChronoUnit.DAYS.between(lr.getFromDate(), lr.getToDate()) + 1;
            if (d < 0) d = 0;
            approvedDays += d;
            if ("UNPAID".equalsIgnoreCase(lr.getType())) {
                unpaidDays += d;
            }
        }

        // Payroll calc
        double gross = base;
        double bonus = 0.0;
        double deduction = daysInMonth > 0 ? (base * ((double) unpaidDays / (double) daysInMonth)) : 0.0;
        if (deduction < 0) deduction = 0.0;
        if (deduction > gross) deduction = gross;
        double net = gross + bonus - deduction;

        return PayrollItem.builder()
                .runId(runIdNullable)
                .shopId(shopId)
                .month(month)
                .staffRef(staffRef)
                .staffType(staffType)
                .branchId(branchId)
                .baseSalary(base)
                .grossSalary(gross)
                .bonus(bonus)
                .deduction(deduction)
                .netSalary(net)
                .workMinutes(workMinutes)
                .approvedLeaveDays(approvedDays)
                .unpaidLeaveDays(unpaidDays)
                .build();
    }

    private PayrollRunResponse toRunResponse(PayrollRun run, List<PayrollItem> items) {
        List<PayrollItemResponse> out = items == null ? List.of() : items.stream().map(this::toItemResponse).toList();
        return PayrollRunResponse.builder()
                .id(run.getId())
                .shopId(run.getShopId())
                .month(run.getMonth())
                .status(run.getStatus())
                .grossTotal(run.getGrossTotal())
                .netTotal(run.getNetTotal())
                .bonusTotal(run.getBonusTotal())
                .deductionTotal(run.getDeductionTotal())
                .finalizedBy(run.getFinalizedBy())
                .finalizedAt(run.getFinalizedAt())
                .paidBy(run.getPaidBy())
                .paidAt(run.getPaidAt())
                .items(out)
                .build();
    }

    private PayrollItemResponse toItemResponse(PayrollItem i) {
        return PayrollItemResponse.builder()
                .id(i.getId())
                .runId(i.getRunId())
                .shopId(i.getShopId())
                .month(i.getMonth())
                .staffRef(i.getStaffRef())
                .staffType(i.getStaffType())
                .branchId(i.getBranchId())
                .baseSalary(i.getBaseSalary())
                .grossSalary(i.getGrossSalary())
                .bonus(i.getBonus())
                .deduction(i.getDeduction())
                .netSalary(i.getNetSalary())
                .workMinutes(i.getWorkMinutes())
                .approvedLeaveDays(i.getApprovedLeaveDays())
                .unpaidLeaveDays(i.getUnpaidLeaveDays())
                .build();
    }

    private static YearMonth parseMonthOrNow(String monthOpt) {
        if (!StringUtils.hasText(monthOpt)) return YearMonth.now();
        try {
            return YearMonth.parse(monthOpt.trim());
        } catch (Exception ex) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
    }

    private static double safe(Double v) {
        return v != null ? v : 0.0;
    }
}

