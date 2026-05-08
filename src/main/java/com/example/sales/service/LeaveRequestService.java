package com.example.sales.service;

import com.example.sales.constant.*;
import com.example.sales.dto.leave.*;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.LeaveRequest;
import com.example.sales.repository.LeaveRequestRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final String TYPE_SYSTEM = "SYSTEM";
    private static final String TYPE_EXTERNAL = "EXTERNAL";

    private final LeaveRequestRepository leaveRequestRepository;
    private final ShopUserRepository shopUserRepository;
    private final StaffProfileRepository staffProfileRepository;

    public LeaveRequestResponse create(String shopId, String actorUserId, LeaveRequestCreateRequest req) {
        if (!StringUtils.hasText(shopId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);
        if (req == null) throw new BusinessException(ApiCode.VALIDATION_ERROR);

        LocalDate from = req.getFromDate();
        LocalDate to = req.getToDate();
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        TargetStaff target = resolveTarget(shopId, actorUserId, req.getStaffRef(), req.getStaffType());

        // Overlap check (simple): any request whose date range intersects
        List<LeaveRequest> existing = leaveRequestRepository
                .findByShopIdAndStaffRefAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDeletedFalse(
                        shopId, target.staffRef, from.minusYears(1), to.plusYears(1));
        for (LeaveRequest lr : existing) {
            if (lr.getStatus() == LeaveRequestStatus.REJECTED || lr.getStatus() == LeaveRequestStatus.CANCELLED) {
                continue;
            }
            if (rangesOverlap(from, to, lr.getFromDate(), lr.getToDate())) {
                throw new BusinessException(ApiCode.LEAVE_REQUEST_OVERLAP);
            }
        }

        LeaveRequest leave = LeaveRequest.builder()
                .shopId(shopId)
                .staffRef(target.staffRef)
                .staffType(target.staffType)
                .branchId(req.getBranchId())
                .type(StringUtils.hasText(req.getType()) ? req.getType().trim() : "ANNUAL")
                .fromDate(from)
                .toDate(to)
                .reason(req.getReason())
                .status(LeaveRequestStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return toResponse(saved);
    }

    public List<LeaveRequestResponse> listForStaff(String shopId, String staffRef, String staffType, String monthOpt) {
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(staffRef)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        YearMonth ym = parseMonthOrNow(monthOpt);
        validateStaffExists(shopId, staffRef, staffType);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        return leaveRequestRepository
                .findByShopIdAndStaffRefAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDeletedFalse(
                        shopId, staffRef, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LeaveRequestResponse approve(String shopId, String leaveId, String approverUserId) {
        LeaveRequest lr = leaveRequestRepository.findByIdAndShopIdAndDeletedFalse(leaveId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.LEAVE_REQUEST_NOT_FOUND));
        if (lr.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BusinessException(ApiCode.LEAVE_REQUEST_INVALID_STATUS);
        }
        lr.setStatus(LeaveRequestStatus.APPROVED);
        lr.setApprovedBy(approverUserId);
        lr.setApprovedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(lr);
        return toResponse(saved);
    }

    public LeaveRequestResponse reject(String shopId, String leaveId, String approverUserId, String reason) {
        LeaveRequest lr = leaveRequestRepository.findByIdAndShopIdAndDeletedFalse(leaveId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.LEAVE_REQUEST_NOT_FOUND));
        if (lr.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BusinessException(ApiCode.LEAVE_REQUEST_INVALID_STATUS);
        }
        lr.setStatus(LeaveRequestStatus.REJECTED);
        lr.setRejectedBy(approverUserId);
        lr.setRejectedAt(LocalDateTime.now());
        lr.setRejectedReason(reason);
        LeaveRequest saved = leaveRequestRepository.save(lr);
        return toResponse(saved);
    }

    public LeaveRequestResponse cancel(String shopId, String leaveId, String actorUserId, String reason) {
        LeaveRequest lr = leaveRequestRepository.findByIdAndShopIdAndDeletedFalse(leaveId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.LEAVE_REQUEST_NOT_FOUND));
        if (lr.getStatus() == LeaveRequestStatus.CANCELLED) {
            return toResponse(lr);
        }
        if (lr.getStatus() == LeaveRequestStatus.APPROVED) {
            // vẫn cho huỷ ở phase 3, nhưng giữ lịch sử
        }
        lr.setStatus(LeaveRequestStatus.CANCELLED);
        lr.setCancelledBy(actorUserId);
        lr.setCancelledAt(LocalDateTime.now());
        if (StringUtils.hasText(reason)) lr.setRejectedReason(reason.trim());
        LeaveRequest saved = leaveRequestRepository.save(lr);
        return toResponse(saved);
    }

    public LeaveMonthSummaryResponse shopMonthSummary(String shopId, String monthOpt) {
        if (!StringUtils.hasText(shopId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);
        YearMonth ym = parseMonthOrNow(monthOpt);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<LeaveRequest> rows = leaveRequestRepository
                .findByShopIdAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDeletedFalse(shopId, from, to);

        long pending = rows.stream().filter(r -> r.getStatus() == LeaveRequestStatus.PENDING).count();
        long approved = rows.stream().filter(r -> r.getStatus() == LeaveRequestStatus.APPROVED).count();
        long rejected = rows.stream().filter(r -> r.getStatus() == LeaveRequestStatus.REJECTED).count();
        long cancelled = rows.stream().filter(r -> r.getStatus() == LeaveRequestStatus.CANCELLED).count();
        Map<String, Long> byType = new HashMap<>();
        for (LeaveRequest r : rows) {
            String t = StringUtils.hasText(r.getType()) ? r.getType() : "ANNUAL";
            byType.merge(t, 1L, Long::sum);
        }
        return LeaveMonthSummaryResponse.builder()
                .shopId(shopId)
                .month(ym.toString())
                .totalRequests(rows.size())
                .pendingRequests(pending)
                .approvedRequests(approved)
                .rejectedRequests(rejected)
                .cancelledRequests(cancelled)
                .byType(byType)
                .build();
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr) {
        return LeaveRequestResponse.builder()
                .id(lr.getId())
                .shopId(lr.getShopId())
                .staffRef(lr.getStaffRef())
                .staffType(lr.getStaffType())
                .branchId(lr.getBranchId())
                .type(lr.getType())
                .fromDate(lr.getFromDate())
                .toDate(lr.getToDate())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .approvedBy(lr.getApprovedBy())
                .approvedAt(lr.getApprovedAt())
                .rejectedBy(lr.getRejectedBy())
                .rejectedAt(lr.getRejectedAt())
                .rejectedReason(lr.getRejectedReason())
                .cancelledBy(lr.getCancelledBy())
                .cancelledAt(lr.getCancelledAt())
                .createdAt(lr.getCreatedAt())
                .updatedAt(lr.getUpdatedAt())
                .build();
    }

    private TargetStaff resolveTarget(String shopId, String actorUserId, String staffRefOpt, String staffTypeOpt) {
        String staffType = normalizeType(staffTypeOpt, TYPE_SYSTEM);
        String staffRef = StringUtils.hasText(staffRefOpt) ? staffRefOpt.trim() : null;

        if (!StringUtils.hasText(staffRef)) {
            staffRef = actorUserId;
            staffType = TYPE_SYSTEM;
        }

        validateStaffExists(shopId, staffRef, staffType);

        // Nếu tạo cho người khác: chỉ OWNER/MANAGER
        if (!staffRef.equals(actorUserId)) {
            ShopRole actorRole = shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, actorUserId)
                    .map(su -> su.getRole())
                    .orElse(null);
            boolean canActForOthers = actorRole == ShopRole.OWNER || actorRole == ShopRole.MANAGER;
            if (!canActForOthers) throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        return new TargetStaff(staffRef, staffType);
    }

    private void validateStaffExists(String shopId, String staffRef, String staffType) {
        if (TYPE_SYSTEM.equals(staffType)) {
            shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, staffRef)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        } else {
            staffProfileRepository.findByIdAndShopIdAndDeletedFalse(staffRef, shopId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND));
        }
    }

    private static boolean rangesOverlap(LocalDate a1, LocalDate a2, LocalDate b1, LocalDate b2) {
        if (a1 == null || a2 == null || b1 == null || b2 == null) return false;
        return !a2.isBefore(b1) && !b2.isBefore(a1);
    }

    private static YearMonth parseMonthOrNow(String monthOpt) {
        if (!StringUtils.hasText(monthOpt)) return YearMonth.now();
        try {
            return YearMonth.parse(monthOpt.trim());
        } catch (Exception ex) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
    }

    private String normalizeType(String t, String defaultType) {
        if (!StringUtils.hasText(t)) return defaultType;
        String s = t.trim().toUpperCase();
        if (TYPE_EXTERNAL.equals(s)) return TYPE_EXTERNAL;
        return TYPE_SYSTEM;
    }

    private record TargetStaff(String staffRef, String staffType) {}
}

