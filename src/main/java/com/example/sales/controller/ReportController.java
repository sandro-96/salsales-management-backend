// File: src/main/java/com/example/sales/controller/ReportController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.report.DailyReportResponse;
import com.example.sales.dto.report.ReportRequest;
import com.example.sales.dto.report.ReportResponse;
import com.example.sales.model.User;
import com.example.sales.service.ReportService;
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

    /**
     * API tổng hợp báo cáo đơn hàng (doanh thu, tổng đơn, số lượng SP)
     */
    @PostMapping
    public ApiResponse<ReportResponse> getReport(@AuthenticationPrincipal User user,
                                                 @RequestBody @Valid ReportRequest request) {
        ReportResponse result = reportService.getReport(user, request);
        return ApiResponse.success(ApiCode.SUCCESS, result);
    }

    /**
     * API thống kê doanh thu theo từng ngày
     */
    @PostMapping("/daily")
    public ApiResponse<List<DailyReportResponse>> getDailyReport(@AuthenticationPrincipal User user,
                                                                 @RequestBody @Valid ReportRequest request) {
        List<DailyReportResponse> result = reportService.getDailyReport(user, request);
        return ApiResponse.success(ApiCode.SUCCESS, result);
    }

    /**
     * API xuất file Excel báo cáo doanh thu theo ngày
     */
    @GetMapping("/daily/export")
    public ResponseEntity<byte[]> exportDailyReport(@AuthenticationPrincipal User user,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.exportDailyReportExcel(user, startDate, endDate);
    }
}
