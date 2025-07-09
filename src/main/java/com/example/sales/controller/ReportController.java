package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.SalesReportDto;
import com.example.sales.model.User;
import com.example.sales.service.ReportService;
import com.example.sales.util.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final MessageService messageService;

    @GetMapping("/sales")
    public ApiResponse<List<SalesReportDto>> getSalesReport(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Locale locale
    ) {
        List<SalesReportDto> report = reportService.getSalesReport(user, from, to);
        return ApiResponse.success(ApiMessage.SALES_REPORT_CREATED, report, messageService, locale);
    }
}
