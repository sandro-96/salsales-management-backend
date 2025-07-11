// File: src/main/java/com/example/sales/helper/ProductSearchHelper.java
package com.example.sales.helper;

import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.model.Product;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Component
@RequiredArgsConstructor
public class ProductSearchHelper {

    private final MongoTemplate mongoTemplate;

    public List<Product> search(String shopId, String branchId, ProductSearchRequest req, Pageable pageable) {
        Aggregation agg = newAggregation(
                buildMatch(shopId, branchId, req),
                sort(Sort.Direction.fromString(req.getSortDir()), req.getSortBy()),
                skip((long) pageable.getPageNumber() * pageable.getPageSize()),
                limit(pageable.getPageSize())
        );

        return mongoTemplate.aggregate(agg, "products", Product.class).getMappedResults();
    }

    public long counts(String shopId, String branchId, ProductSearchRequest req) {
        Aggregation agg = newAggregation(
                buildMatch(shopId, branchId, req),
                count().as("total")
        );

        return Optional.of(
                mongoTemplate.aggregate(agg, "products", Document.class).getUniqueMappedResult()
        ).map(d -> ((Number) d.get("total")).longValue()).orElse(0L);
    }

    private MatchOperation buildMatch(String shopId, String branchId, ProductSearchRequest req) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("shopId").is(shopId));
        if (branchId != null && !branchId.isBlank()) {
            criteria.add(Criteria.where("branchId").is(branchId));
        }
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String pattern = ".*" + req.getKeyword().trim() + ".*";
            criteria.add(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern, "i"),
                    Criteria.where("category").regex(pattern, "i")
            ));
        }

        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            criteria.add(Criteria.where("category").is(req.getCategory()));
        }

        if (req.getActive() != null) {
            criteria.add(Criteria.where("active").is(req.getActive()));
        }

        if (req.getMinPrice() != null) {
            criteria.add(Criteria.where("price").gte(req.getMinPrice()));
        }

        if (req.getMaxPrice() != null) {
            criteria.add(Criteria.where("price").lte(req.getMaxPrice()));
        }

        return match(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
    }
}
