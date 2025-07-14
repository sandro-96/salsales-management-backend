// File: src/main/java/com/example/sales/service/ReportService.java
package com.example.sales.service;

import com.example.sales.dto.report.DailyReportResponse;
import com.example.sales.dto.report.ReportRequest;
import com.example.sales.dto.report.ReportResponse;
import com.example.sales.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MongoTemplate mongoTemplate;
    private final ExcelExportService excelExportService;

    public ReportResponse getReport(String shopId, ReportRequest request) {
        MatchOperation match = buildMatchOperation(shopId, request);

        Aggregation aggregation = newAggregation(
                match,
                unwind("items"),
                group()
                        .count().as("totalOrders")
                        .sum("items.quantity").as("totalProductsSold")
                        .sum("totalPrice").as("totalRevenue")
        );

        AggregationResults<ReportResponse> results =
                mongoTemplate.aggregate(aggregation, Order.class, ReportResponse.class);

        ReportResponse response = results.getUniqueMappedResult();

        return response != null ? response : ReportResponse.builder()
                .totalOrders(0)
                .totalProductsSold(0)
                .totalRevenue(0)
                .build();
    }

    public List<DailyReportResponse> getDailyReport(String shopId, ReportRequest request) {
        MatchOperation match = buildMatchOperation(shopId, request);

        ProjectionOperation projectDate = project()
                .andExpression("year(createdAt)").as("year")
                .andExpression("month(createdAt)").as("month")
                .andExpression("dayOfMonth(createdAt)").as("day")
                .and("totalPrice").as("totalPrice")
                .and("items").as("items");

        Aggregation aggregation = newAggregation(
                match,
                projectDate,
                unwind("items"),
                group("year", "month", "day")
                        .count().as("totalOrders")
                        .sum("items.quantity").as("totalProductsSold")
                        .sum("totalPrice").as("totalRevenue"),
                project()
                        .andExpression("dateFromParts(year: _id.year, month: _id.month, day: _id.day)").as("date")
                        .andInclude("totalOrders", "totalProductsSold", "totalRevenue"),
                sort(Sort.Direction.ASC, "date")
        );

        AggregationResults<DailyReportResponse> results =
                mongoTemplate.aggregate(aggregation, Order.class, DailyReportResponse.class);

        return results.getMappedResults();
    }

    public ResponseEntity<byte[]> exportDailyReportExcel(String shopId,
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportRequest request = new ReportRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        List<DailyReportResponse> data = getDailyReport(shopId, request);

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return excelExportService.exportExcel(
                "daily-report.xlsx",
                "Báo cáo theo ngày",
                List.of("Ngày", "Tổng đơn", "Tổng sản phẩm", "Tổng doanh thu"),
                data,
                r -> List.of(
                        r.getDate().format(df),
                        String.valueOf(r.getTotalOrders()),
                        String.valueOf(r.getTotalProductsSold()),
                        String.valueOf(r.getTotalRevenue())
                )
        );
    }

    private MatchOperation buildMatchOperation(String shopId, ReportRequest request) {
        Criteria criteria = Criteria.where("shopId").is(shopId);

        if (request.getStatus() != null) {
            criteria = criteria.and("status").is(request.getStatus());
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            LocalDateTime start = request.getStartDate().atStartOfDay();
            LocalDateTime end = request.getEndDate().atTime(LocalTime.MAX);
            criteria = criteria.and("createdAt").gte(start).lte(end);
        }

        return match(criteria);
    }
}
