package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.report.DailyReportResponse;
import com.example.sales.dto.report.ReportRequest;
import com.example.sales.dto.report.ReportResponse;
import com.example.sales.dto.report.TopProductResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.security.RequirePlan;
import com.example.sales.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    //@RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping("/summary")
    @RequirePermission(Permission.REPORT_VIEW)
    @Operation(
            summary = "Lấy báo cáo tổng hợp",
            description = "Trả về báo cáo tổng hợp doanh thu, số đơn hàng, sản phẩm đã bán, giá trị trung bình đơn hàng trong khoảng thời gian chỉ định"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Báo cáo tổng hợp được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ")
    })
    public ApiResponseDto<ReportResponse> getReport(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin yêu cầu báo cáo") ReportRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, reportService.getReport(shopId, request));
    }

    //@RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping("/daily")
    @RequirePermission(Permission.REPORT_VIEW)
    @Operation(
            summary = "Lấy báo cáo theo ngày",
            description = "Trả về danh sách báo cáo chi tiết theo từng ngày trong khoảng thời gian chỉ định"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Báo cáo theo ngày được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ")
    })
    public ApiResponseDto<List<DailyReportResponse>> getDaily(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin yêu cầu báo cáo theo ngày") ReportRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, reportService.getDailyReport(shopId, request));
    }

    //@RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping("/top-products")
    @RequirePermission(Permission.REPORT_VIEW)
    @Operation(
            summary = "Lấy sản phẩm bán chạy",
            description = "Trả về danh sách sản phẩm bán chạy nhất trong khoảng thời gian chỉ định"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách sản phẩm bán chạy được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ")
    })
    public ApiResponseDto<List<TopProductResponse>> getTopProducts(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam(defaultValue = "10") @Parameter(description = "Số lượng sản phẩm (mặc định 10)") int limit,
            @RequestBody @Valid @Parameter(description = "Thông tin yêu cầu báo cáo") ReportRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, reportService.getTopProducts(shopId, request, limit));
    }

    //@RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @GetMapping("/daily/export")
    @RequirePermission(Permission.REPORT_VIEW)
    @Operation(
            summary = "Xuất báo cáo theo ngày ra file Excel",
            description = "Xuất dữ liệu báo cáo theo ngày trong khoảng thời gian chỉ định dưới dạng file Excel"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File Excel được xuất thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ")
    })
    public ResponseEntity<byte[]> exportDaily(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)") LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)") LocalDate endDate,
            @RequestParam(required = false) @Parameter(description = "ID chi nhánh (tuỳ chọn)") String branchId) {
        return reportService.exportDailyReportExcel(shopId, startDate, endDate, branchId);
    }

    //@RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping("/top-products/export")
    @RequirePermission(Permission.REPORT_VIEW)
    @Operation(
            summary = "Xuất sản phẩm bán chạy ra file Excel",
            description = "Xuất danh sách sản phẩm bán chạy nhất dưới dạng file Excel"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File Excel được xuất thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ")
    })
    public ResponseEntity<byte[]> exportTopProducts(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam(defaultValue = "10") @Parameter(description = "Số lượng sản phẩm (mặc định 10)") int limit,
            @RequestBody @Valid @Parameter(description = "Thông tin yêu cầu báo cáo") ReportRequest request) {
        return reportService.exportTopProductsExcel(shopId, request, limit);
    }
}
