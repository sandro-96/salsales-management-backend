package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.report.DailyReportResponse;
import com.example.sales.dto.report.ReportRequest;
import com.example.sales.dto.report.ReportResponse;
import com.example.sales.model.User;
import com.example.sales.service.ReportService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final MessageService messageService;

    /**
     * API tổng hợp báo cáo đơn hàng (doanh thu, tổng đơn, số lượng SP)
     */
    @PostMapping
    public ApiResponse<ReportResponse> getReport(@AuthenticationPrincipal User user,
                                                 @RequestBody @Valid ReportRequest request,
                                                 Locale locale) {
        ReportResponse result = reportService.getReport(user, request);
        return ApiResponse.success(ApiMessage.SUCCESS, result, messageService, locale);
    }

    /**
     * API thống kê doanh thu theo từng ngày
     */
    @PostMapping("/daily")
    public ApiResponse<List<DailyReportResponse>> getDailyReport(@AuthenticationPrincipal User user,
                                                                 @RequestBody @Valid ReportRequest request,
                                                                 Locale locale) {
        List<DailyReportResponse> result = reportService.getDailyReport(user, request);
        return ApiResponse.success(ApiMessage.SUCCESS, result, messageService, locale);
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
