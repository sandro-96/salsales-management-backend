package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.staffProfile.StaffProfileRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.StaffProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops/{shopId}/staff-profiles")
@RequiredArgsConstructor
@Validated
public class StaffProfileController {

    private final StaffProfileService staffProfileService;

    // ─── Unified list ─────────────────────────────────────────────────

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Danh sách tất cả nhân sự",
            description = "Trả về danh sách gộp: nhân viên hệ thống (có tài khoản) + nhân viên ngoài hệ thống (tạp vụ, bưng bê...).")
    public ApiResponseDto<?> getAllStaff(
            @PathVariable String shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String branchId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponseDto.success(ApiCode.SUCCESS,
                staffProfileService.getAllStaff(shopId, keyword, branchId, pageable));
    }

    // ─── System user profile (linked to userId) ──────────────────────

    @GetMapping("/{userId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Lấy hồ sơ nhân sự theo userId",
            description = "Lấy thông tin hồ sơ nhân sự của một nhân viên hệ thống. Trả về dữ liệu rỗng nếu chưa tạo hồ sơ.")
    public ApiResponseDto<?> getProfile(
            @PathVariable String shopId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, staffProfileService.getProfile(shopId, userId));
    }

    @PutMapping("/{userId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Tạo hoặc cập nhật hồ sơ nhân sự theo userId",
            description = "Upsert hồ sơ cho nhân viên hệ thống.")
    public ApiResponseDto<?> saveProfile(
            @PathVariable String shopId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody StaffProfileRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                staffProfileService.createOrUpdateProfile(shopId, userId, request, customUserDetails.getId()));
    }

    @DeleteMapping("/{userId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Xoá hồ sơ nhân sự theo userId (soft delete)")
    public ApiResponseDto<?> deleteProfile(
            @PathVariable String shopId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        staffProfileService.deleteProfile(shopId, userId, customUserDetails.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    // ─── External staff (no system account) ──────────────────────────

    @PostMapping("/external")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Tạo nhân viên ngoài hệ thống",
            description = "Tạo hồ sơ nhân sự cho người không cần tài khoản hệ thống (tạp vụ, bưng bê...).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Thiếu họ tên"),
            @ApiResponse(responseCode = "403", description = "Không có quyền")
    })
    public ApiResponseDto<?> createExternalProfile(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody StaffProfileRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                staffProfileService.createExternalProfile(shopId, request, customUserDetails.getId()));
    }

    @GetMapping("/external/{profileId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Lấy hồ sơ nhân viên ngoài hệ thống theo ID")
    public ApiResponseDto<?> getExternalProfile(
            @PathVariable String shopId,
            @PathVariable String profileId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                staffProfileService.getExternalProfile(shopId, profileId));
    }

    @PutMapping("/external/{profileId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Cập nhật hồ sơ nhân viên ngoài hệ thống")
    public ApiResponseDto<?> updateExternalProfile(
            @PathVariable String shopId,
            @PathVariable String profileId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody StaffProfileRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                staffProfileService.updateExternalProfile(shopId, profileId, request, customUserDetails.getId()));
    }

    @DeleteMapping("/external/{profileId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Xoá nhân viên ngoài hệ thống (soft delete)")
    public ApiResponseDto<?> deleteExternalProfile(
            @PathVariable String shopId,
            @PathVariable String profileId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        staffProfileService.deleteExternalProfile(shopId, profileId, customUserDetails.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    // ─── Export ───────────────────────────────────────────────────────

    @GetMapping("/export")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Xuất hồ sơ nhân sự ra Excel",
            description = "Xuất tất cả nhân sự (hệ thống + ngoài hệ thống) ra file Excel.")
    public ResponseEntity<byte[]> exportProfiles(
            @PathVariable String shopId,
            @RequestParam(required = false) String branchId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return staffProfileService.exportProfiles(shopId, branchId);
    }
}
