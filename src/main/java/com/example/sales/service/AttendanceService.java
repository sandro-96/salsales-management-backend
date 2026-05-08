package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.attendance.*;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.AttendanceLog;
import com.example.sales.repository.AttendanceLogRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final String TYPE_SYSTEM = "SYSTEM";
    private static final String TYPE_EXTERNAL = "EXTERNAL";

    private final AttendanceLogRepository attendanceLogRepository;
    private final ShopUserRepository shopUserRepository;
    private final StaffProfileRepository staffProfileRepository;

    public AttendanceEntryResponse checkIn(String shopId, String actorUserId, AttendanceCheckInRequest req) {
        if (!StringUtils.hasText(shopId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);
        LocalDate today = LocalDate.now();

        TargetStaff target = resolveTarget(shopId, actorUserId, req != null ? req.getStaffRef() : null,
                req != null ? req.getStaffType() : null);

        AttendanceLog log = attendanceLogRepository
                .findByShopIdAndStaffRefAndWorkDateAndDeletedFalse(shopId, target.staffRef, today)
                .orElseGet(() -> AttendanceLog.builder()
                        .shopId(shopId)
                        .staffRef(target.staffRef)
                        .staffType(target.staffType)
                        .workDate(today)
                        .build());

        LocalDateTime now = LocalDateTime.now();
        ensureSessionsFromLegacy(log);
        AttendanceLog.AttendanceSession last = lastSession(log);
        // Nếu session cuối chưa check-out => idempotent (đã check-in)
        if (last != null && last.getCheckInAt() != null && last.getCheckOutAt() == null) {
            // keep
        } else {
            log.getSessions().add(AttendanceLog.AttendanceSession.builder()
                    .checkInAt(now)
                    .build());
        }
        // Backward-compatible snapshot
        if (log.getCheckInAt() == null) log.setCheckInAt(now);
        if (req != null) {
            if (StringUtils.hasText(req.getBranchId())) log.setBranchId(req.getBranchId().trim());
            if (StringUtils.hasText(req.getNote())) {
                log.setNote(req.getNote().trim());
                AttendanceLog.AttendanceSession cur = lastSession(log);
                if (cur != null) cur.setNote(req.getNote().trim());
            }
        }
        AttendanceLog saved = attendanceLogRepository.save(log);
        return toEntry(saved);
    }

    public AttendanceEntryResponse checkOut(String shopId, String actorUserId, AttendanceCheckOutRequest req) {
        if (!StringUtils.hasText(shopId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);
        LocalDate today = LocalDate.now();

        TargetStaff target = resolveTarget(shopId, actorUserId, req != null ? req.getStaffRef() : null,
                req != null ? req.getStaffType() : null);

        AttendanceLog log = attendanceLogRepository
                .findByShopIdAndStaffRefAndWorkDateAndDeletedFalse(shopId, target.staffRef, today)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));

        ensureSessionsFromLegacy(log);
        AttendanceLog.AttendanceSession last = lastSession(log);
        if (log.getCheckInAt() == null && (last == null || last.getCheckInAt() == null)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        // Idempotent: nếu session cuối đã check-out => trả lại
        if (last != null && last.getCheckInAt() != null && last.getCheckOutAt() == null) {
            last.setCheckOutAt(LocalDateTime.now());
        } else if (log.getCheckOutAt() == null && log.getCheckInAt() != null) {
            // Fallback legacy
            log.setCheckOutAt(LocalDateTime.now());
        }
        // Backward-compatible snapshot
        if (log.getCheckOutAt() == null) log.setCheckOutAt(LocalDateTime.now());

        if (req != null && StringUtils.hasText(req.getNote())) {
            log.setNote(req.getNote().trim());
            AttendanceLog.AttendanceSession cur = lastSession(log);
            if (cur != null) cur.setNote(req.getNote().trim());
        }
        AttendanceLog saved = attendanceLogRepository.save(log);
        return toEntry(saved);
    }

    public AttendanceDaySummaryResponse daySummary(String shopId, LocalDate date) {
        if (!StringUtils.hasText(shopId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);
        LocalDate d = date != null ? date : LocalDate.now();
        List<AttendanceLog> rows = attendanceLogRepository.findByShopIdAndWorkDateAndDeletedFalse(shopId, d);
        long checkedIn = rows.stream().filter(r -> r.getCheckInAt() != null).count();
        long checkedOut = rows.stream().filter(r -> r.getCheckOutAt() != null).count();
        return AttendanceDaySummaryResponse.builder()
                .shopId(shopId)
                .workDate(d)
                .totalEntries(rows.size())
                .checkedInCount(checkedIn)
                .checkedOutCount(checkedOut)
                .entries(rows.stream().map(this::toEntry).toList())
                .build();
    }

    public AttendanceMonthSummaryResponse staffMonthSummary(String shopId, String staffRef, String staffTypeOpt, String month) {
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(staffRef)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        YearMonth ym;
        try {
            ym = month != null && !month.isBlank() ? YearMonth.parse(month.trim()) : YearMonth.now();
        } catch (Exception ex) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        String staffType = normalizeType(staffTypeOpt, TYPE_SYSTEM);
        // Validate existence lightly (Phase 2)
        if (TYPE_SYSTEM.equals(staffType)) {
            shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, staffRef)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        } else {
            staffProfileRepository.findByIdAndShopIdAndDeletedFalse(staffRef, shopId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND));
        }

        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<AttendanceLog> rows = attendanceLogRepository
                .findByShopIdAndStaffRefAndWorkDateBetweenAndDeletedFalse(shopId, staffRef, from, to);
        long checkedInDays = rows.stream().filter(r -> r.getCheckInAt() != null).count();
        long checkedOutDays = rows.stream().filter(r -> r.getCheckOutAt() != null).count();
        return AttendanceMonthSummaryResponse.builder()
                .shopId(shopId)
                .month(ym.toString())
                .staffRef(staffRef)
                .staffType(staffType)
                .totalDays(rows.size())
                .checkedInDays(checkedInDays)
                .checkedOutDays(checkedOutDays)
                .entries(rows.stream().map(this::toEntry).toList())
                .build();
    }

    private AttendanceEntryResponse toEntry(AttendanceLog a) {
        ensureSessionsFromLegacy(a);
        List<AttendanceEntryResponse.AttendanceSessionResponse> sessions = a.getSessions() == null
                ? List.of()
                : a.getSessions().stream().map(s -> {
                    Long minutes = null;
                    if (s.getCheckInAt() != null && s.getCheckOutAt() != null) {
                        minutes = ChronoUnit.MINUTES.between(s.getCheckInAt(), s.getCheckOutAt());
                        if (minutes < 0) minutes = 0L;
                    }
                    return AttendanceEntryResponse.AttendanceSessionResponse.builder()
                            .checkInAt(s.getCheckInAt())
                            .checkOutAt(s.getCheckOutAt())
                            .note(s.getNote())
                            .minutes(minutes)
                            .build();
                }).toList();
        long totalMinutes = sessions.stream()
                .map(AttendanceEntryResponse.AttendanceSessionResponse::getMinutes)
                .filter(m -> m != null && m > 0)
                .mapToLong(Long::longValue)
                .sum();
        return AttendanceEntryResponse.builder()
                .id(a.getId())
                .shopId(a.getShopId())
                .staffRef(a.getStaffRef())
                .staffType(a.getStaffType())
                .branchId(a.getBranchId())
                .workDate(a.getWorkDate())
                .checkInAt(a.getCheckInAt())
                .checkOutAt(a.getCheckOutAt())
                .note(a.getNote())
                .sessions(sessions)
                .totalMinutes(totalMinutes)
                .build();
    }

    private void ensureSessionsFromLegacy(AttendanceLog log) {
        if (log == null) return;
        if (log.getSessions() != null && !log.getSessions().isEmpty()) return;
        if (log.getCheckInAt() == null && log.getCheckOutAt() == null) return;
        if (log.getSessions() == null) {
            log.setSessions(new java.util.ArrayList<>());
        }
        log.getSessions().add(AttendanceLog.AttendanceSession.builder()
                .checkInAt(log.getCheckInAt())
                .checkOutAt(log.getCheckOutAt())
                .note(log.getNote())
                .build());
    }

    private AttendanceLog.AttendanceSession lastSession(AttendanceLog log) {
        if (log == null || log.getSessions() == null || log.getSessions().isEmpty()) return null;
        return log.getSessions().get(log.getSessions().size() - 1);
    }

    private TargetStaff resolveTarget(
            String shopId,
            String actorUserId,
            String staffRefOpt,
            String staffTypeOpt
    ) {
        String staffType = normalizeType(staffTypeOpt, TYPE_SYSTEM);
        String staffRef = StringUtils.hasText(staffRefOpt) ? staffRefOpt.trim() : null;

        // Default: staff tự check-in/out cho mình
        if (!StringUtils.hasText(staffRef)) {
            if (!StringUtils.hasText(actorUserId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);
            shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, actorUserId)
                    .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED));
            return new TargetStaff(actorUserId, TYPE_SYSTEM);
        }

        // Nếu chấm công cho người khác: chỉ OWNER/MANAGER
        ShopRole actorRole = shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, actorUserId)
                .map(su -> su.getRole())
                .orElse(null);
        boolean canActForOthers = actorRole == ShopRole.OWNER || actorRole == ShopRole.MANAGER;
        if (!canActForOthers) {
            // Nếu staffRef = chính mình thì cho phép
            if (TYPE_SYSTEM.equals(staffType) && staffRef.equals(actorUserId)) {
                return new TargetStaff(actorUserId, TYPE_SYSTEM);
            }
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        if (TYPE_SYSTEM.equals(staffType)) {
            shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, staffRef)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        } else {
            staffProfileRepository.findByIdAndShopIdAndDeletedFalse(staffRef, shopId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND));
        }
        return new TargetStaff(staffRef, staffType);
    }

    private String normalizeType(String t, String defaultType) {
        if (!StringUtils.hasText(t)) return defaultType;
        String s = t.trim().toUpperCase();
        if (TYPE_EXTERNAL.equals(s)) return TYPE_EXTERNAL;
        return TYPE_SYSTEM;
    }

    private record TargetStaff(String staffRef, String staffType) {}
}

