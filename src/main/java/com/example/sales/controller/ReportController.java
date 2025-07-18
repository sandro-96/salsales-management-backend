// File: src/main/java/com/example/sales/controller/ReportController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.report.DailyReportResponse;
import com.example.sales.dto.report.ReportRequest;
import com.example.sales.dto.report.ReportResponse;
import com.example.sales.security.RequirePlan;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final ReportService reportService;

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping("/summary")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(
            summary = "Lấy báo cáo tổng hợp",
            description = "Trả về báo cáo tổng hợp doanh thu, số lượng đơn hàng, sản phẩm đã bán,... trong khoảng thời gian được chỉ định"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Báo cáo tổng hợp được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng hoặc dữ liệu báo cáo")
    })
    public ApiResponseDto<ReportResponse> getReport(
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin yêu cầu báo cáo") ReportRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, reportService.getReport(shopId, request));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping("/daily")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(
            summary = "Lấy báo cáo theo ngày",
            description = "Trả về danh sách báo cáo chi tiết theo từng ngày trong khoảng thời gian chỉ định"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Báo cáo theo ngày được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng hoặc dữ liệu báo cáo")
    })
    public ApiResponseDto<List<DailyReportResponse>> getDaily(
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin yêu cầu báo cáo theo ngày") ReportRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, reportService.getDailyReport(shopId, request));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @GetMapping("/daily/export")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(
            summary = "Xuất báo cáo theo ngày ra file Excel",
            description = "Xuất dữ liệu báo cáo theo ngày trong khoảng thời gian chỉ định dưới dạng file Excel"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File Excel được xuất thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng hoặc dữ liệu báo cáo")
    })
    public ResponseEntity<byte[]> exportDaily(
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)") LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)") LocalDate endDate) {
        return reportService.exportDailyReportExcel(shopId, startDate, endDate);
    }
}
