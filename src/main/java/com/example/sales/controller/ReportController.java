// File: src/main/java/com/example/sales/controller/ReportController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.report.DailyReportResponse;
import com.example.sales.dto.report.ReportRequest;
import com.example.sales.dto.report.ReportResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ReportService;
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

    @PostMapping("/summary")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<ReportResponse> getReport(@RequestParam String shopId,
                                                 @RequestBody ReportRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, reportService.getReport(shopId, request));
    }

    @PostMapping("/daily")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<DailyReportResponse>> getDaily(@RequestParam String shopId,
                                                           @RequestBody ReportRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, reportService.getDailyReport(shopId, request));
    }

    @GetMapping("/daily/export")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<byte[]> exportDaily(@RequestParam String shopId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.exportDailyReportExcel(shopId, startDate, endDate);
    }
}
