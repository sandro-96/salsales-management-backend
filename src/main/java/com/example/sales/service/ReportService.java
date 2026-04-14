package com.example.sales.service;

import com.example.sales.constant.OrderStatus;
import com.example.sales.dto.report.DailyReportResponse;
import com.example.sales.dto.report.ReportRequest;
import com.example.sales.dto.report.ReportResponse;
import com.example.sales.dto.report.TopProductResponse;
import com.example.sales.model.Order;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
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
                project()
                        .and("totalPrice").as("totalPrice")
                        .and("totalAmount").as("totalAmount")
                        .and(context -> new Document("$sum", "$items.quantity")).as("itemCount"),
                group()
                        .count().as("totalOrders")
                        .sum("itemCount").as("totalProductsSold")
                        .sum("totalPrice").as("totalRevenue")
                        .sum("totalAmount").as("totalAmount")
        );

        AggregationResults<ReportResponse> results =
                mongoTemplate.aggregate(aggregation, Order.class, ReportResponse.class);

        ReportResponse response = results.getUniqueMappedResult();

        if (response == null) {
            return ReportResponse.builder()
                    .totalOrders(0)
                    .totalProductsSold(0)
                    .totalRevenue(0)
                    .totalAmount(0)
                    .averageOrderValue(0)
                    .build();
        }

        response.setAverageOrderValue(
                response.getTotalOrders() > 0
                        ? Math.round(response.getTotalRevenue() / response.getTotalOrders() * 100.0) / 100.0
                        : 0
        );
        return response;
    }

    public List<DailyReportResponse> getDailyReport(String shopId, ReportRequest request) {
        MatchOperation match = buildMatchOperation(shopId, request);

        Aggregation aggregation = newAggregation(
                match,
                project()
                        .andExpression("year(createdAt)").as("year")
                        .andExpression("month(createdAt)").as("month")
                        .andExpression("dayOfMonth(createdAt)").as("day")
                        .and("totalPrice").as("totalPrice")
                        .and("totalAmount").as("totalAmount")
                        .and(context -> new Document("$sum", "$items.quantity")).as("itemCount"),
                group("year", "month", "day")
                        .count().as("totalOrders")
                        .sum("itemCount").as("totalProductsSold")
                        .sum("totalPrice").as("totalRevenue")
                        .sum("totalAmount").as("totalAmount"),
                project()
                        .and(context -> new Document("$dateFromParts",
                                new Document("year", "$_id.year")
                                        .append("month", "$_id.month")
                                        .append("day", "$_id.day"))).as("date")
                        .andInclude("totalOrders", "totalProductsSold", "totalRevenue", "totalAmount"),
                sort(Sort.Direction.ASC, "date")
        );

        AggregationResults<DailyReportResponse> results =
                mongoTemplate.aggregate(aggregation, Order.class, DailyReportResponse.class);

        return results.getMappedResults();
    }

    public List<TopProductResponse> getTopProducts(String shopId, ReportRequest request, int limit) {
        MatchOperation match = buildMatchOperation(shopId, request);

        Aggregation aggregation = newAggregation(
                match,
                unwind("items"),
                group("items.productId")
                        .first("items.productName").as("productName")
                        .sum("items.quantity").as("totalQuantitySold")
                        .sum(context -> new Document("$multiply",
                                List.of("$items.quantity", "$items.priceAfterDiscount"))).as("totalRevenue"),
                sort(Sort.Direction.DESC, "totalQuantitySold"),
                limit(limit),
                project()
                        .and("_id").as("productId")
                        .andInclude("productName", "totalQuantitySold", "totalRevenue")
        );

        return mongoTemplate.aggregate(aggregation, Order.class, TopProductResponse.class).getMappedResults();
    }

    public ResponseEntity<byte[]> exportDailyReportExcel(String shopId, LocalDate startDate,
                                                         LocalDate endDate, String branchId) {
        ReportRequest request = new ReportRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setBranchId(branchId);

        List<DailyReportResponse> data = getDailyReport(shopId, request);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return excelExportService.exportExcel(
                "daily-report.xlsx",
                "Báo cáo theo ngày",
                List.of("Ngày", "Tổng đơn", "Tổng sản phẩm", "Doanh thu", "Tổng tiền (gồm thuế)"),
                data,
                r -> List.of(
                        r.getDate().format(df),
                        String.valueOf(r.getTotalOrders()),
                        String.valueOf(r.getTotalProductsSold()),
                        String.valueOf(r.getTotalRevenue()),
                        String.valueOf(r.getTotalAmount())
                )
        );
    }

    public ResponseEntity<byte[]> exportTopProductsExcel(String shopId, ReportRequest request, int limit) {
        List<TopProductResponse> data = getTopProducts(shopId, request, limit);

        return excelExportService.exportExcel(
                "top-products.xlsx",
                "Sản phẩm bán chạy",
                List.of("Mã sản phẩm", "Tên sản phẩm", "Số lượng bán", "Doanh thu"),
                data,
                r -> List.of(
                        r.getProductId(),
                        r.getProductName(),
                        String.valueOf(r.getTotalQuantitySold()),
                        String.valueOf(r.getTotalRevenue())
                )
        );
    }

    private MatchOperation buildMatchOperation(String shopId, ReportRequest request) {
        Criteria criteria = Criteria.where("shopId").is(shopId);

        if (request.getBranchId() != null && !request.getBranchId().isBlank()) {
            criteria = criteria.and("branchId").is(request.getBranchId());
        }

        if (request.getStatus() != null) {
            criteria = criteria.and("status").is(request.getStatus());
        } else {
            // Tổng quan / báo cáo không truyền status: không tính doanh thu đơn đã hủy
            criteria = criteria.and("status").ne(OrderStatus.CANCELLED);
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            LocalDateTime start = request.getStartDate().atStartOfDay();
            LocalDateTime end = request.getEndDate().atTime(LocalTime.MAX);
            criteria = criteria.and("createdAt").gte(start).lte(end);
        }

        return match(criteria);
    }
}
