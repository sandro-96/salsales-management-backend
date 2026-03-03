// File: src/main/java/com/example/sales/helper/ProductSearchHelper.java
package com.example.sales.helper;

import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.model.BranchProduct;
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

    /**
     * Tìm kiếm BranchProduct (join với Product để filter theo name/category/sku/barcode).
     * Query trên collection branch_products, lookup sang products để lấy thông tin chung.
     */
    public List<BranchProduct> search(String shopId, String branchId, ProductSearchRequest req, Pageable pageable) {
        Aggregation agg = newAggregation(
                buildBranchMatch(shopId, branchId),
                lookup("products", "productId", "_id", "productDetail"),
                unwind("productDetail"),
                buildProductFilter(req),
                sort(Sort.Direction.fromString(req.getSortDir()), resolveSortField(req.getSortBy())),
                skip((long) pageable.getPageNumber() * pageable.getPageSize()),
                limit(pageable.getPageSize())
        );
        return mongoTemplate.aggregate(agg, "branch_products", BranchProduct.class).getMappedResults();
    }

    public long count(String shopId, String branchId, ProductSearchRequest req) {
        Aggregation agg = newAggregation(
                buildBranchMatch(shopId, branchId),
                lookup("products", "productId", "_id", "productDetail"),
                unwind("productDetail"),
                buildProductFilter(req),
                group().count().as("total")
        );
        return Optional.ofNullable(
                mongoTemplate.aggregate(agg, "branch_products", Document.class).getUniqueMappedResult()
        ).map(d -> ((Number) d.get("total")).longValue()).orElse(0L);
    }

    /** Match các điều kiện thuộc BranchProduct (shopId, branchId, active, price range, deleted) */
    private MatchOperation buildBranchMatch(String shopId, String branchId) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("shopId").is(shopId));
        criteria.add(Criteria.where("deleted").ne(true));
        if (branchId != null && !branchId.isBlank()) {
            criteria.add(Criteria.where("branchId").is(branchId));
        }
        return match(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
    }

    /** Match các điều kiện filter từ request (keyword, category, active, price range) */
    private MatchOperation buildProductFilter(ProductSearchRequest req) {
        List<Criteria> criteria = new ArrayList<>();

        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String pattern = ".*" + req.getKeyword().trim() + ".*";
            criteria.add(new Criteria().orOperator(
                    Criteria.where("productDetail.name").regex(pattern, "i"),
                    Criteria.where("productDetail.sku").regex(pattern, "i"),
                    Criteria.where("productDetail.barcode").regex(pattern, "i")
            ));
        }

        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            criteria.add(Criteria.where("productDetail.category").is(req.getCategory()));
        }

        if (req.getActive() != null) {
            // activeInBranch là field của BranchProduct
            criteria.add(Criteria.where("activeInBranch").is(req.getActive()));
        }

        if (req.getMinPrice() != null) {
            criteria.add(Criteria.where("price").gte(req.getMinPrice()));
        }

        if (req.getMaxPrice() != null) {
            criteria.add(Criteria.where("price").lte(req.getMaxPrice()));
        }

        if (criteria.isEmpty()) {
            return match(new Criteria());
        }
        return match(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
    }

    /** Map sortBy từ request sang field thực tế trong BranchProduct/productDetail */
    private String resolveSortField(String sortBy) {
        return switch (sortBy) {
            case "name" -> "productDetail.name";
            case "price" -> "price";
            case "quantity" -> "quantity";
            case "category" -> "productDetail.category";
            default -> "createdAt";
        };
    }
}
