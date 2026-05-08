package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ContractType;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.staffProfile.StaffMemberOverviewResponse;
import com.example.sales.dto.staffProfile.StaffProfileRequest;
import com.example.sales.dto.staffProfile.StaffProfileResponse;
import com.example.sales.dto.staffProfile.StaffShopOverviewResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.model.ShopUser;
import com.example.sales.model.StaffProfile;
import com.example.sales.model.User;
import com.example.sales.model.AttendanceLog;
import com.example.sales.repository.AttendanceLogRepository;
import com.example.sales.repository.PayrollRunRepository;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.StaffProfileRepository;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffProfileService {

    private final StaffProfileRepository staffProfileRepository;
    private final ShopUserRepository shopUserRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;

    // ─── Unified list: system users + external staff ──────────────────

    public Page<StaffProfileResponse> getAllStaff(String shopId, String keyword, String branchId, Pageable pageable) {
        List<ShopUser> shopUsers = shopUserRepository
                .findByShopIdAndDeletedFalse(shopId, Pageable.unpaged())
                .getContent();

        List<String> systemUserIds = shopUsers.stream().map(ShopUser::getUserId).toList();

        Map<String, User> userMap = userRepository.findAllById(systemUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<String, ShopRole> roleMap = shopUsers.stream()
                .collect(Collectors.toMap(ShopUser::getUserId, ShopUser::getRole, (a, b) -> a));
        Map<String, ShopUser> shopUserMap = shopUsers.stream()
                .collect(Collectors.toMap(ShopUser::getUserId, Function.identity(), (a, b) -> a));

        List<StaffProfile> allProfiles = staffProfileRepository.findByShopIdAndDeletedFalse(shopId);
        Map<String, StaffProfile> profileByUserId = allProfiles.stream()
                .filter(p -> p.getUserId() != null)
                .collect(Collectors.toMap(StaffProfile::getUserId, Function.identity(), (a, b) -> a));

        List<StaffProfile> externalProfiles = allProfiles.stream()
                .filter(p -> p.getUserId() == null)
                .toList();

        List<StaffProfileResponse> results = new ArrayList<>();

        for (ShopUser su : shopUsers) {
            User user = userMap.getOrDefault(su.getUserId(), new User());
            StaffProfile profile = profileByUserId.get(su.getUserId());
            results.add(buildSystemUserResponse(su, user, profile));
        }

        for (StaffProfile ep : externalProfiles) {
            results.add(buildExternalResponse(ep));
        }

        if (branchId != null && !branchId.isBlank()) {
            results = results.stream()
                    .filter(r -> branchId.equals(r.getBranchId()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase().trim();
            results = results.stream()
                    .filter(r -> matchesKeyword(r, kw))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        results.sort(Comparator.comparing(
                StaffProfileResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int total = results.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<StaffProfileResponse> page = start < total ? results.subList(start, end) : List.of();

        return new PageImpl<>(page, pageable, total);
    }

    private boolean matchesKeyword(StaffProfileResponse r, String kw) {
        return (r.getFullName() != null && r.getFullName().toLowerCase().contains(kw))
                || (r.getEmail() != null && r.getEmail().toLowerCase().contains(kw))
                || (r.getPhone() != null && r.getPhone().toLowerCase().contains(kw))
                || (r.getPosition() != null && r.getPosition().toLowerCase().contains(kw));
    }

    private StaffProfileResponse buildSystemUserResponse(ShopUser su, User user, StaffProfile profile) {
        StaffProfileResponse.StaffProfileResponseBuilder b = StaffProfileResponse.builder()
                .shopId(su.getShopId())
                .userId(su.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(su.getRole())
                .external(false)
                .createdAt(su.getCreatedAt());

        if (profile != null) {
            b.id(profile.getId())
                    .branchId(profile.getBranchId())
                    .position(profile.getPosition())
                    .department(profile.getDepartment())
                    .level(profile.getLevel())
                    .startDate(profile.getStartDate())
                    .salary(profile.getSalary())
                    .contractType(profile.getContractType())
                    .idNumber(profile.getIdNumber())
                    .bankName(profile.getBankName())
                    .bankAccountNumber(profile.getBankAccountNumber())
                    .bankAccountHolder(profile.getBankAccountHolder())
                    .emergencyContactName(profile.getEmergencyContactName())
                    .emergencyContactPhone(profile.getEmergencyContactPhone())
                    .note(profile.getNote())
                    .updatedAt(profile.getUpdatedAt());
        }

        return b.build();
    }

    private StaffProfileResponse buildExternalResponse(StaffProfile p) {
        return StaffProfileResponse.builder()
                .id(p.getId())
                .shopId(p.getShopId())
                .branchId(p.getBranchId())
                .fullName(p.getFullName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .role(null)
                .external(true)
                .position(p.getPosition())
                .department(p.getDepartment())
                .level(p.getLevel())
                .startDate(p.getStartDate())
                .salary(p.getSalary())
                .contractType(p.getContractType())
                .idNumber(p.getIdNumber())
                .bankName(p.getBankName())
                .bankAccountNumber(p.getBankAccountNumber())
                .bankAccountHolder(p.getBankAccountHolder())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .note(p.getNote())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    // ─── System user profile (linked to userId) ──────────────────────

    public StaffProfileResponse getProfile(String shopId, String userId) {
        StaffProfile profile = staffProfileRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .orElse(null);

        User user = userRepository.findByIdAndDeletedFalse(userId).orElse(new User());
        ShopRole role = shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(ShopUser::getRole)
                .orElse(null);

        if (profile == null) {
            return StaffProfileResponse.builder()
                    .shopId(shopId)
                    .userId(userId)
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatarUrl(user.getAvatarUrl())
                    .role(role)
                    .external(false)
                    .build();
        }

        return toLinkedResponse(profile, user, role);
    }

    public StaffProfileResponse createOrUpdateProfile(String shopId, String userId, StaffProfileRequest request, String performedByUserId) {
        StaffProfile profile = staffProfileRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .orElse(null);

        boolean isNew = (profile == null);

        if (isNew) {
            profile = new StaffProfile();
            profile.setShopId(shopId);
            profile.setUserId(userId);
        }

        applyRequestToProfile(profile, request);
        StaffProfile saved = staffProfileRepository.save(profile);

        String action = isNew ? "CREATED" : "UPDATED";
        auditLogService.log(performedByUserId, shopId, saved.getId(), "STAFF_PROFILE", action,
                String.format("%s hồ sơ nhân sự cho người dùng %s", isNew ? "Tạo" : "Cập nhật", userId));

        User user = userRepository.findByIdAndDeletedFalse(userId).orElse(new User());
        ShopRole role = shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(ShopUser::getRole)
                .orElse(null);

        return toLinkedResponse(saved, user, role);
    }

    public void deleteProfile(String shopId, String userId, String performedByUserId) {
        StaffProfile profile = staffProfileRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND));

        profile.setDeleted(true);
        staffProfileRepository.save(profile);

        auditLogService.log(performedByUserId, shopId, profile.getId(), "STAFF_PROFILE", "DELETED",
                String.format("Xoá hồ sơ nhân sự cho người dùng %s", userId));
    }

    // ─── External staff profile (no system account) ──────────────────

    public StaffProfileResponse createExternalProfile(String shopId, StaffProfileRequest request, String performedByUserId) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        StaffProfile profile = new StaffProfile();
        profile.setShopId(shopId);
        profile.setFullName(request.getFullName().trim());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        applyRequestToProfile(profile, request);

        StaffProfile saved = staffProfileRepository.save(profile);

        auditLogService.log(performedByUserId, shopId, saved.getId(), "STAFF_PROFILE", "CREATED",
                String.format("Tạo hồ sơ nhân sự ngoài hệ thống: %s", saved.getFullName()));

        return buildExternalResponse(saved);
    }

    public StaffProfileResponse getExternalProfile(String shopId, String profileId) {
        StaffProfile profile = staffProfileRepository.findByIdAndShopIdAndDeletedFalse(profileId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND));
        if (profile.getUserId() != null) {
            return getProfile(shopId, profile.getUserId());
        }
        return buildExternalResponse(profile);
    }

    public StaffProfileResponse updateExternalProfile(String shopId, String profileId, StaffProfileRequest request, String performedByUserId) {
        StaffProfile profile = staffProfileRepository.findByIdAndShopIdAndDeletedFalse(profileId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND));

        if (profile.getUserId() != null) {
            return createOrUpdateProfile(shopId, profile.getUserId(), request, performedByUserId);
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            profile.setFullName(request.getFullName().trim());
        }
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        applyRequestToProfile(profile, request);

        StaffProfile saved = staffProfileRepository.save(profile);

        auditLogService.log(performedByUserId, shopId, saved.getId(), "STAFF_PROFILE", "UPDATED",
                String.format("Cập nhật hồ sơ nhân sự ngoài hệ thống: %s", saved.getFullName()));

        return buildExternalResponse(saved);
    }

    public void deleteExternalProfile(String shopId, String profileId, String performedByUserId) {
        StaffProfile profile = staffProfileRepository.findByIdAndShopIdAndDeletedFalse(profileId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND));

        profile.setDeleted(true);
        staffProfileRepository.save(profile);

        String name = profile.getFullName() != null ? profile.getFullName() : profileId;
        auditLogService.log(performedByUserId, shopId, profile.getId(), "STAFF_PROFILE", "DELETED",
                String.format("Xoá hồ sơ nhân sự: %s", name));
    }

    // ─── Export ───────────────────────────────────────────────────────

    public ResponseEntity<byte[]> exportProfiles(String shopId, String branchId) {
        List<StaffProfile> profiles;
        if (branchId != null && !branchId.isBlank()) {
            profiles = staffProfileRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId);
        } else {
            profiles = staffProfileRepository.findByShopIdAndDeletedFalse(shopId);
        }

        List<String> userIds = profiles.stream()
                .map(StaffProfile::getUserId)
                .filter(Objects::nonNull)
                .toList();
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<String, ShopRole> roleMap = shopUserRepository
                .findByShopIdAndDeletedFalse(shopId, Pageable.unpaged())
                .getContent().stream()
                .collect(Collectors.toMap(ShopUser::getUserId, ShopUser::getRole, (a, b) -> a));

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return excelExportService.exportExcel(
                "staff_profiles.xlsx",
                "Hồ sơ nhân sự",
                List.of("Họ tên", "Email", "SĐT", "Vai trò", "Loại", "Vị trí", "Phòng ban",
                        "Cấp bậc", "Ngày vào làm", "Lương", "Loại HĐ", "Số CCCD",
                        "Ngân hàng", "STK", "Chủ TK", "LH khẩn cấp", "SĐT khẩn cấp", "Ghi chú"),
                profiles,
                p -> {
                    boolean isExternal = p.getUserId() == null;
                    String fullName, email, phone;
                    ShopRole r = null;

                    if (isExternal) {
                        fullName = safe(p.getFullName());
                        email = safe(p.getEmail());
                        phone = safe(p.getPhone());
                    } else {
                        User u = userMap.getOrDefault(p.getUserId(), new User());
                        fullName = safe(u.getFullName());
                        email = safe(u.getEmail());
                        phone = safe(u.getPhone());
                        r = roleMap.get(p.getUserId());
                    }

                    return List.of(
                            fullName,
                            email,
                            phone,
                            r != null ? r.name() : "",
                            isExternal ? "Ngoài hệ thống" : "Hệ thống",
                            safe(p.getPosition()),
                            safe(p.getDepartment()),
                            safe(p.getLevel()),
                            p.getStartDate() != null ? p.getStartDate().format(df) : "",
                            p.getSalary() != null ? String.valueOf(p.getSalary().longValue()) : "",
                            p.getContractType() != null ? p.getContractType().name() : "",
                            safe(p.getIdNumber()),
                            safe(p.getBankName()),
                            safe(p.getBankAccountNumber()),
                            safe(p.getBankAccountHolder()),
                            safe(p.getEmergencyContactName()),
                            safe(p.getEmergencyContactPhone()),
                            safe(p.getNote())
                    );
                }
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private void applyRequestToProfile(StaffProfile profile, StaffProfileRequest request) {
        profile.setBranchId(request.getBranchId());
        profile.setPosition(request.getPosition());
        profile.setDepartment(request.getDepartment());
        profile.setLevel(request.getLevel());
        profile.setStartDate(request.getStartDate());
        profile.setSalary(request.getSalary());
        profile.setContractType(request.getContractType());
        profile.setIdNumber(request.getIdNumber());
        profile.setBankName(request.getBankName());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankAccountHolder(request.getBankAccountHolder());
        profile.setEmergencyContactName(request.getEmergencyContactName());
        profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        profile.setNote(request.getNote());
    }

    private StaffProfileResponse toLinkedResponse(StaffProfile p, User user, ShopRole role) {
        return StaffProfileResponse.builder()
                .id(p.getId())
                .shopId(p.getShopId())
                .userId(p.getUserId())
                .branchId(p.getBranchId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(role)
                .external(false)
                .position(p.getPosition())
                .department(p.getDepartment())
                .level(p.getLevel())
                .startDate(p.getStartDate())
                .salary(p.getSalary())
                .contractType(p.getContractType())
                .idNumber(p.getIdNumber())
                .bankName(p.getBankName())
                .bankAccountNumber(p.getBankAccountNumber())
                .bankAccountHolder(p.getBankAccountHolder())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .note(p.getNote())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    // ─── Overview / Dashboard (Phase 1) ───────────────────────────────

    /**
     * Tổng quan nhân sự cấp shop dùng cho trang Staff Dashboard.
     *
     * <p>Phase 1 chỉ trả các metric tổng quan (số lượng + chi phí lương ước tính).
     * Các block <i>attendance</i>, <i>leave</i>, <i>payroll</i> trả về placeholder
     * (enabled=false) để phase 2–4 bật mà không phải đổi schema FE.
     */
    public StaffShopOverviewResponse getShopOverview(String shopId, String monthOpt) {
        YearMonth ym = parseMonthOrNow(monthOpt);
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<ShopUser> shopUsers = shopUserRepository
                .findByShopIdAndDeletedFalse(shopId, Pageable.unpaged())
                .getContent();
        List<StaffProfile> profiles = staffProfileRepository.findByShopIdAndDeletedFalse(shopId);

        Map<String, StaffProfile> profileByUserId = profiles.stream()
                .filter(p -> p.getUserId() != null)
                .collect(Collectors.toMap(StaffProfile::getUserId, Function.identity(), (a, b) -> a));
        List<StaffProfile> externalProfiles = profiles.stream()
                .filter(p -> p.getUserId() == null)
                .toList();

        long systemStaff = shopUsers.size();
        long externalStaff = externalProfiles.size();
        long totalStaff = systemStaff + externalStaff;

        Map<ShopRole, Long> staffByRole = new EnumMap<>(ShopRole.class);
        for (ShopRole r : ShopRole.values()) staffByRole.put(r, 0L);
        for (ShopUser su : shopUsers) {
            if (su.getRole() == null) continue;
            staffByRole.merge(su.getRole(), 1L, Long::sum);
        }

        // Lấy tên branch để hiển thị; dùng cache 1 query
        Map<String, String> branchNameById = new HashMap<>();
        for (Branch b : branchRepository.findAllByShopIdAndDeletedFalse(shopId)) {
            branchNameById.put(b.getId(), b.getName());
        }

        Map<String, Long> branchCount = new LinkedHashMap<>();
        for (StaffProfile p : profiles) {
            String key = p.getBranchId() != null ? p.getBranchId() : "__unassigned__";
            branchCount.merge(key, 1L, Long::sum);
        }
        // Người trong shopUsers nhưng không có profile vẫn cần được đếm vào "chưa gán chi nhánh"
        long usersWithoutProfile = shopUsers.stream()
                .filter(su -> !profileByUserId.containsKey(su.getUserId()))
                .count();
        if (usersWithoutProfile > 0) {
            branchCount.merge("__unassigned__", usersWithoutProfile, Long::sum);
        }
        List<StaffShopOverviewResponse.BranchStaffCount> branchList = branchCount.entrySet().stream()
                .map(e -> StaffShopOverviewResponse.BranchStaffCount.builder()
                        .branchId("__unassigned__".equals(e.getKey()) ? null : e.getKey())
                        .branchName("__unassigned__".equals(e.getKey())
                                ? "Chưa gán chi nhánh"
                                : branchNameById.getOrDefault(e.getKey(), e.getKey()))
                        .count(e.getValue())
                        .build())
                .sorted(Comparator
                        .comparing((StaffShopOverviewResponse.BranchStaffCount b) -> b.getBranchId() == null)
                        .thenComparing(StaffShopOverviewResponse.BranchStaffCount::getBranchName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        double totalSalary = 0.0;
        long staffWithSalary = 0L;
        Map<ContractType, StaffShopOverviewResponse.ContractTypeBreakdown> byContractAcc = new EnumMap<>(ContractType.class);
        Map<ContractType, Long> contractStaffCount = new EnumMap<>(ContractType.class);
        Map<ContractType, Double> contractSalarySum = new EnumMap<>(ContractType.class);

        for (StaffProfile p : profiles) {
            Double salary = p.getSalary();
            if (salary != null && salary > 0) {
                totalSalary += salary;
                staffWithSalary += 1;
            }
            ContractType ct = p.getContractType();
            if (ct != null) {
                contractStaffCount.merge(ct, 1L, Long::sum);
                if (salary != null && salary > 0) {
                    contractSalarySum.merge(ct, salary, Double::sum);
                }
            }
        }
        for (Map.Entry<ContractType, Long> e : contractStaffCount.entrySet()) {
            byContractAcc.put(e.getKey(), StaffShopOverviewResponse.ContractTypeBreakdown.builder()
                    .staffCount(e.getValue())
                    .totalSalary(contractSalarySum.getOrDefault(e.getKey(), 0.0))
                    .build());
        }
        double averageSalary = staffWithSalary > 0 ? totalSalary / staffWithSalary : 0.0;

        long newStaffThisMonth = 0L;
        for (ShopUser su : shopUsers) {
            if (su.getCreatedAt() != null
                    && !su.getCreatedAt().isBefore(monthStart)
                    && su.getCreatedAt().isBefore(nextMonthStart)) {
                newStaffThisMonth += 1;
            }
        }
        for (StaffProfile p : externalProfiles) {
            if (p.getCreatedAt() != null
                    && !p.getCreatedAt().isBefore(monthStart)
                    && p.getCreatedAt().isBefore(nextMonthStart)) {
                newStaffThisMonth += 1;
            }
        }

        // Phase 2: chấm công (tối thiểu) — tổng hợp theo tháng
        LocalDate fromDate = ym.atDay(1);
        LocalDate toDate = ym.atEndOfMonth();
        List<AttendanceLog> attendanceRows = new ArrayList<>();
        // SYSTEM users
        for (ShopUser su : shopUsers) {
            if (su.getUserId() == null) continue;
            attendanceRows.addAll(attendanceLogRepository
                    .findByShopIdAndStaffRefAndWorkDateBetweenAndDeletedFalse(shopId, su.getUserId(), fromDate, toDate));
        }
        // EXTERNAL staff
        for (StaffProfile ep : externalProfiles) {
            if (ep.getId() == null) continue;
            attendanceRows.addAll(attendanceLogRepository
                    .findByShopIdAndStaffRefAndWorkDateBetweenAndDeletedFalse(shopId, ep.getId(), fromDate, toDate));
        }
        long totalSessions = 0L;
        long completedSessions = 0L;
        long totalWorkMinutes = 0L;
        for (AttendanceLog log : attendanceRows) {
            if (log.getSessions() != null && !log.getSessions().isEmpty()) {
                for (AttendanceLog.AttendanceSession s : log.getSessions()) {
                    if (s.getCheckInAt() == null) continue;
                    totalSessions += 1;
                    if (s.getCheckOutAt() != null) {
                        completedSessions += 1;
                        long m = java.time.temporal.ChronoUnit.MINUTES.between(s.getCheckInAt(), s.getCheckOutAt());
                        if (m > 0) totalWorkMinutes += m;
                    }
                }
            } else if (log.getCheckInAt() != null) {
                // Legacy record (Phase 2): treat as 1 session
                totalSessions += 1;
                if (log.getCheckOutAt() != null) {
                    completedSessions += 1;
                    long m = java.time.temporal.ChronoUnit.MINUTES.between(log.getCheckInAt(), log.getCheckOutAt());
                    if (m > 0) totalWorkMinutes += m;
                }
            }
        }

        return StaffShopOverviewResponse.builder()
                .month(ym.toString())
                .totalStaff(totalStaff)
                .systemStaff(systemStaff)
                .externalStaff(externalStaff)
                .staffByRole(staffByRole)
                .staffByBranch(branchList)
                .totalMonthlySalary(round2(totalSalary))
                .averageSalary(round2(averageSalary))
                .staffWithSalary(staffWithSalary)
                .payrollByContract(byContractAcc)
                .newStaffThisMonth(newStaffThisMonth)
                .attendance(StaffShopOverviewResponse.AttendanceSummary.builder()
                        .enabled(true)
                        .totalShiftsScheduled(totalSessions)
                        .totalShiftsCompleted(completedSessions)
                        .totalLateArrivals(0L)
                        .totalEarlyLeaves(0L)
                        .totalWorkMinutes(totalWorkMinutes)
                        .build())
                .leave(StaffShopOverviewResponse.LeaveSummary.builder().enabled(true).build())
                .payroll(buildPayrollSummary(shopId, ym.toString()))
                .build();
    }

    private StaffShopOverviewResponse.PayrollSummary buildPayrollSummary(String shopId, String month) {
        return payrollRunRepository.findByShopIdAndMonthAndDeletedFalse(shopId, month)
                .map(run -> StaffShopOverviewResponse.PayrollSummary.builder()
                        .enabled(true)
                        .grossPayroll(run.getGrossTotal())
                        .netPayroll(run.getNetTotal())
                        .bonusTotal(run.getBonusTotal())
                        .deductionTotal(run.getDeductionTotal())
                        .status(run.getStatus() != null ? run.getStatus().name() : null)
                        .build())
                .orElseGet(() -> StaffShopOverviewResponse.PayrollSummary.builder()
                        .enabled(false)
                        .grossPayroll(0.0)
                        .netPayroll(0.0)
                        .bonusTotal(0.0)
                        .deductionTotal(0.0)
                        .status("NOT_GENERATED")
                        .build());
    }

    /**
     * Tổng quan của 1 nhân sự cụ thể.
     *
     * <p>Hỗ trợ 2 dạng id:
     * <ul>
     *   <li>userId hệ thống (ShopUser tồn tại)</li>
     *   <li>profileId của nhân sự ngoài hệ thống</li>
     * </ul>
     */
    public StaffMemberOverviewResponse getStaffMemberOverview(
            String shopId, String userOrProfileId, String monthOpt) {
        if (userOrProfileId == null || userOrProfileId.isBlank()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        YearMonth ym = parseMonthOrNow(monthOpt);

        StaffProfileResponse profileResponse = null;

        // Ưu tiên thử userId hệ thống.
        Optional<ShopUser> shopUserOpt = shopUserRepository
                .findByShopIdAndUserIdAndDeletedFalse(shopId, userOrProfileId);
        if (shopUserOpt.isPresent()) {
            profileResponse = getProfile(shopId, userOrProfileId);
        } else {
            // Fallback: profileId (external hoặc system).
            StaffProfile profile = staffProfileRepository
                    .findByIdAndShopIdAndDeletedFalse(userOrProfileId, shopId)
                    .orElse(null);
            if (profile != null) {
                if (profile.getUserId() != null) {
                    profileResponse = getProfile(shopId, profile.getUserId());
                } else {
                    profileResponse = buildExternalResponse(profile);
                }
            }
        }

        if (profileResponse == null) {
            throw new ResourceNotFoundException(ApiCode.STAFF_PROFILE_NOT_FOUND);
        }

        Double baseSalary = profileResponse.getSalary();

        return StaffMemberOverviewResponse.builder()
                .profile(profileResponse)
                .month(ym.toString())
                .attendance(StaffMemberOverviewResponse.AttendanceBlock.builder()
                        .enabled(false)
                        .recentEntries(List.of())
                        .build())
                .leave(StaffMemberOverviewResponse.LeaveBlock.builder()
                        .enabled(false)
                        .recentRequests(List.of())
                        .build())
                .payroll(StaffMemberOverviewResponse.PayrollBlock.builder()
                        .enabled(false)
                        .baseSalary(baseSalary)
                        .recentRuns(List.of())
                        .build())
                .build();
    }

    private static YearMonth parseMonthOrNow(String monthOpt) {
        if (monthOpt == null || monthOpt.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(monthOpt.trim());
        } catch (Exception ex) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
