package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.staffProfile.StaffProfileRequest;
import com.example.sales.dto.staffProfile.StaffProfileResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.ShopUser;
import com.example.sales.model.StaffProfile;
import com.example.sales.model.User;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.StaffProfileRepository;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
}
