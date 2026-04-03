package com.example.sales.helper;

import com.example.sales.dto.customer.CustomerSearchRequest;
import com.example.sales.model.Customer;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Component
public class CustomerSearchHelper {

    private final MongoTemplate mongoTemplate;

    public CustomerSearchHelper(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Customer> search(String shopId, String branchId, CustomerSearchRequest req, Pageable pageable) {
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(req.getSortDir())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Aggregation agg = newAggregation(
                buildMatch(shopId, branchId, req),
                sort(direction, sortBy),
                skip((long) pageable.getPageNumber() * pageable.getPageSize()),
                limit(pageable.getPageSize())
        );

        return mongoTemplate.aggregate(agg, "customers", Customer.class).getMappedResults();
    }

    public long count(String shopId, String branchId, CustomerSearchRequest req) {
        Aggregation countAgg = newAggregation(
                buildMatch(shopId, branchId, req),
                Aggregation.count().as("total")
        );

        return Optional.ofNullable(
                mongoTemplate.aggregate(countAgg, "customers", Document.class)
                        .getUniqueMappedResult()
        ).map(d -> ((Number) d.get("total")).longValue()).orElse(0L);
    }

    public List<Customer> exportAll(String shopId, String branchId, CustomerSearchRequest req) {
        Aggregation agg = newAggregation(
                buildMatch(shopId, branchId, req),
                sort(Sort.Direction.ASC, "name")
        );

        return mongoTemplate.aggregate(agg, "customers", Customer.class).getMappedResults();
    }

    private MatchOperation buildMatch(String shopId, String branchId, CustomerSearchRequest req) {
        String keyword = Optional.ofNullable(req.getKeyword()).orElse("").trim();

        List<Criteria> andConditions = new ArrayList<>();
        andConditions.add(Criteria.where("shopId").is(shopId));
        andConditions.add(Criteria.where("deleted").is(false));

        if (branchId != null && !branchId.isBlank()) {
            andConditions.add(Criteria.where("branchId").is(branchId));
        }

        if (!keyword.isEmpty()) {
            andConditions.add(new Criteria().orOperator(
                    Criteria.where("name").regex(keyword, "i"),
                    Criteria.where("email").regex(keyword, "i"),
                    Criteria.where("phone").regex(keyword, "i")
            ));
        }

        if (req.getFromDate() != null || req.getToDate() != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (req.getFromDate() != null) {
                dateCriteria = dateCriteria.gte(req.getFromDate().atStartOfDay());
            }
            if (req.getToDate() != null) {
                dateCriteria = dateCriteria.lte(req.getToDate().atTime(23, 59, 59));
            }
            andConditions.add(dateCriteria);
        }

        return match(new Criteria().andOperator(andConditions.toArray(new Criteria[0])));
    }
}
