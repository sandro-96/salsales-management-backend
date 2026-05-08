package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.leave.LeaveRequestActionRequest;
import com.example.sales.dto.leave.LeaveRequestCreateRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops/{shopId}/leave-requests")
@RequiredArgsConstructor
@Validated
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER})
    @Operation(summary = "Tạo đơn nghỉ phép (Phase 3)")
    public ApiResponseDto<?> create(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody LeaveRequestCreateRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                leaveRequestService.create(shopId, customUserDetails.getId(), request));
    }

    @GetMapping("/staff/{staffRef}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Danh sách đơn nghỉ phép theo nhân sự + tháng (Phase 3)")
    public ApiResponseDto<?> listForStaff(
            @PathVariable String shopId,
            @PathVariable String staffRef,
            @RequestParam(required = false) String staffType,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                leaveRequestService.listForStaff(shopId, staffRef, staffType, month));
    }

    @PostMapping("/{leaveId}/approve")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Duyệt đơn nghỉ phép (Phase 3)")
    public ApiResponseDto<?> approve(
            @PathVariable String shopId,
            @PathVariable String leaveId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                leaveRequestService.approve(shopId, leaveId, customUserDetails.getId()));
    }

    @PostMapping("/{leaveId}/reject")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Từ chối đơn nghỉ phép (Phase 3)")
    public ApiResponseDto<?> reject(
            @PathVariable String shopId,
            @PathVariable String leaveId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody(required = false) LeaveRequestActionRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        return ApiResponseDto.success(ApiCode.SUCCESS,
                leaveRequestService.reject(shopId, leaveId, customUserDetails.getId(), reason));
    }

    @PostMapping("/{leaveId}/cancel")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER})
    @Operation(summary = "Huỷ đơn nghỉ phép (Phase 3)")
    public ApiResponseDto<?> cancel(
            @PathVariable String shopId,
            @PathVariable String leaveId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody(required = false) LeaveRequestActionRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        return ApiResponseDto.success(ApiCode.SUCCESS,
                leaveRequestService.cancel(shopId, leaveId, customUserDetails.getId(), reason));
    }

    @GetMapping("/summary")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Tổng hợp nghỉ phép theo tháng (Phase 3)")
    public ApiResponseDto<?> shopMonthSummary(
            @PathVariable String shopId,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                leaveRequestService.shopMonthSummary(shopId, month));
    }
}

