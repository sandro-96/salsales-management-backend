// File: src/main/java/com/example/sales/helper/CustomerSearchHelper.java
package com.example.sales.helper;

import com.example.sales.dto.customer.CustomerSearchRequest;
import com.example.sales.model.Customer;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
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

    public List<Customer> search(String userId, CustomerSearchRequest req, Pageable pageable) {
        Aggregation agg = newAggregation(
                buildMatch(userId, req),
                sort(Sort.Direction.DESC, "_id"),
                skip((long) pageable.getPageNumber() * pageable.getPageSize()),
                limit(pageable.getPageSize())
        );

        return mongoTemplate.aggregate(agg, "customers", Customer.class).getMappedResults();
    }

    public long counts(String userId, CustomerSearchRequest req) {
        Aggregation countAgg = newAggregation(
                buildMatch(userId, req),
                count().as("total")
        );

        return Optional.of(
                mongoTemplate.aggregate(countAgg, "customers", Document.class)
                        .getUniqueMappedResult()
        ).map(d -> ((Number) d.get("total")).longValue()).orElse(0L);
    }

    public List<Customer> exportAll(String userId, CustomerSearchRequest req) {
        Aggregation agg = newAggregation(
                buildMatch(userId, req),
                sort(Sort.Direction.ASC, "name")
        );

        return mongoTemplate.aggregate(agg, "customers", Customer.class).getMappedResults();
    }

    private MatchOperation buildMatch(String userId, CustomerSearchRequest req) {
        String keyword = Optional.ofNullable(req.getKeyword()).orElse("").trim();

        Criteria base = Criteria.where("userId").is(userId);
        List<Criteria> andConditions = new ArrayList<>();
        andConditions.add(base);

        // keyword
        if (!keyword.isEmpty()) {
            andConditions.add(new Criteria().orOperator(
                    Criteria.where("name").regex(keyword, "i"),
                    Criteria.where("email").regex(keyword, "i"),
                    Criteria.where("phone").regex(keyword, "i")
            ));
        }

        // filter ngày tạo
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
