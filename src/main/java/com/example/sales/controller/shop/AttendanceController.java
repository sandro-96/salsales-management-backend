package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.attendance.AttendanceCheckInRequest;
import com.example.sales.dto.attendance.AttendanceCheckOutRequest;
import com.example.sales.dto.attendance.AttendanceManualSessionRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shops/{shopId}/attendance")
@RequiredArgsConstructor
@Validated
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER})
    @Operation(summary = "Check-in (Phase 2)", description = "Chấm công check-in cho hôm nay. Staff tự check-in nếu không truyền staffRef.")
    public ApiResponseDto<?> checkIn(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody(required = false) AttendanceCheckInRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                attendanceService.checkIn(shopId, customUserDetails.getId(), request));
    }

    @PostMapping("/check-out")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER})
    @Operation(summary = "Check-out (Phase 2)", description = "Chấm công check-out cho hôm nay. Staff tự check-out nếu không truyền staffRef.")
    public ApiResponseDto<?> checkOut(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody(required = false) AttendanceCheckOutRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                attendanceService.checkOut(shopId, customUserDetails.getId(), request));
    }

    @PostMapping("/manual-session")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Nhập giờ chấm công (Owner/Manager)", description = "Nhập giờ vào / giờ ra theo ngày cho nhân viên. replaceDay=true ghi đè cả ngày.")
    public ApiResponseDto<?> manualSession(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody AttendanceManualSessionRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                attendanceService.upsertManualSession(shopId, customUserDetails.getId(), request));
    }

    @GetMapping("/day")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Tổng hợp chấm công theo ngày (Phase 2)")
    public ApiResponseDto<?> daySummary(
            @PathVariable String shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, attendanceService.daySummary(shopId, date));
    }

    @GetMapping("/staff/{staffRef}/month")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Tổng hợp chấm công theo tháng của 1 nhân sự (Phase 2)")
    public ApiResponseDto<?> staffMonthSummary(
            @PathVariable String shopId,
            @PathVariable String staffRef,
            @RequestParam(required = false) String staffType,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                attendanceService.staffMonthSummary(shopId, staffRef, staffType, month));
    }

}

