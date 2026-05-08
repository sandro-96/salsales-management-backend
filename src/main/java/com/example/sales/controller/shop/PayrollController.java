package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops/{shopId}/payroll")
@RequiredArgsConstructor
@Validated
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/generate")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Tạo / tính lại bảng lương theo tháng (Phase 4)")
    public ApiResponseDto<?> generate(
            @PathVariable String shopId,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                payrollService.generateOrRecompute(shopId, month, customUserDetails.getId()));
    }

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Xem bảng lương theo tháng (Phase 4)")
    public ApiResponseDto<?> getRun(
            @PathVariable String shopId,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, payrollService.getRun(shopId, month));
    }

    @PostMapping("/finalize")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Chốt bảng lương theo tháng (Phase 4)")
    public ApiResponseDto<?> finalizeRun(
            @PathVariable String shopId,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                payrollService.finalizeRun(shopId, month, customUserDetails.getId()));
    }

    @GetMapping("/staff/{staffRef}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Xem payroll item của 1 nhân sự theo tháng (Phase 4)")
    public ApiResponseDto<?> getStaffMonth(
            @PathVariable String shopId,
            @PathVariable String staffRef,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                payrollService.getStaffMonthItem(shopId, month, staffRef));
    }
}

