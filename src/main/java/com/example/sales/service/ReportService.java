package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.dto.SalesReportDto;
import com.example.sales.model.Order;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MongoTemplate mongoTemplate;
    private final ShopRepository shopRepository;

    public List<SalesReportDto> getSalesReport(User user, LocalDate from, LocalDate to) {
        Shop shop = shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException(ApiErrorCode.SHOP_NOT_FOUND.getMessage()));

        // Convert to Date
        Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()); // inclusive

        MatchOperation match = Aggregation.match(Criteria.where("shopId").is(shop.getId())
                .and("createdAt").gte(fromDate).lt(toDate));

        ProjectionOperation project = Aggregation.project()
                .andExpression("year(createdAt)").as("year")
                .andExpression("month(createdAt)").as("month")
                .andExpression("dayOfMonth(createdAt)").as("day")
                .and("totalAmount").as("totalAmount");

        GroupOperation group = Aggregation.group("year", "month", "day")
                .count().as("totalOrders")
                .sum("totalAmount").as("totalAmount");

        ProjectionOperation formatDate = Aggregation.project()
                .andExpression("dateToString('%Y-%m-%d', dateFromParts({ year: $_id.year, month: $_id.month, day: $_id.day }))").as("date")
                .and("totalOrders").as("totalOrders")
                .and("totalAmount").as("totalAmount");

        Aggregation aggregation = Aggregation.newAggregation(match, project, group, formatDate,
                Aggregation.sort(Sort.Direction.ASC, "date"));

        return mongoTemplate.aggregate(aggregation, Order.class, SalesReportDto.class).getMappedResults();
    }
}
