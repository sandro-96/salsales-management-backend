// File: src/main/java/com/example/sales/helper/ProductSearchHelper.java
package com.example.sales.helper;

import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper tìm kiếm sản phẩm theo 2 bước tách biệt:
 *
 * Bước 1 — findMatchingProductIds():
 *   Tìm productIds từ collection "products" theo các tiêu chí thuộc Product
 *   (keyword: name/sku/barcode, category). Trả về null nếu không có filter → lấy tất cả.
 *
 * Bước 2 — searchBranchProducts() / countBranchProducts():
 *   Lọc BranchProduct theo (shopId, branchId, productIds, price range, activeInBranch).
 *   productIds = null → không lọc theo sản phẩm cụ thể.
 */
@Component
@RequiredArgsConstructor
public class ProductSearchHelper {

    private final MongoTemplate mongoTemplate;

    /**
     * Bước 1: Tìm productIds từ collection "products" theo keyword và category.
     * Trả về null nếu không có filter cấp Product (nghĩa là không giới hạn productId).
     * Trả về Set rỗng nếu có filter nhưng không có kết quả → sẽ không query tiếp BranchProduct.
     */
    public Set<String> findMatchingProductIds(String shopId, ProductSearchRequest req) {
        boolean hasKeyword = StringUtils.hasText(req.getKeyword());
        boolean hasCategory = StringUtils.hasText(req.getCategory());

        if (!hasKeyword && !hasCategory) return null; // Không có filter Product → lấy tất cả

        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("shopId").is(shopId));
        criteriaList.add(Criteria.where("deleted").ne(true));

        if (hasKeyword) {
            String pattern = ".*" + req.getKeyword().trim() + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern, "i"),
                    Criteria.where("sku").regex(pattern, "i"),
                    Criteria.where("barcode").regex(pattern, "i")
            ));
        }

        if (hasCategory) {
            criteriaList.add(Criteria.where("category").is(req.getCategory()));
        }

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        query.fields().include("_id"); // Chỉ lấy ID để tối ưu

        return mongoTemplate.find(query, Product.class).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Bước 2: Tìm BranchProduct theo các tiêu chí thuộc BranchProduct
     * (shopId, branchId, price range, activeInBranch) + productIds từ bước 1.
     *
     * @param productIds null = không lọc theo productId; Set rỗng = không có kết quả nào
     */
    public List<BranchProduct> searchBranchProducts(String shopId, String branchId,
                                                     Set<String> productIds,
                                                     ProductSearchRequest req, Pageable pageable) {
        if (productIds != null && productIds.isEmpty()) return List.of();

        Query query = new Query(buildBranchCriteria(shopId, branchId, productIds, req))
                .with(Sort.by(Sort.Direction.fromString(req.getSortDir()), resolveSortField(req.getSortBy())))
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize());

        return mongoTemplate.find(query, BranchProduct.class, "branch_products");
    }

    /**
     * Đếm tổng BranchProduct khớp với criteria (dùng cho phân trang).
     *
     * @param productIds null = không lọc theo productId; Set rỗng = trả về 0
     */
    public long countBranchProducts(String shopId, String branchId,
                                     Set<String> productIds, ProductSearchRequest req) {
        if (productIds != null && productIds.isEmpty()) return 0;

        return mongoTemplate.count(
                new Query(buildBranchCriteria(shopId, branchId, productIds, req)),
                BranchProduct.class, "branch_products"
        );
    }

    /**
     * Xây dựng Criteria cho BranchProduct:
     * - shopId, branchId, deleted
     * - productId in productIds (nếu không null)
     * - activeInBranch, price range
     */
    private Criteria buildBranchCriteria(String shopId, String branchId,
                                          Set<String> productIds, ProductSearchRequest req) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("shopId").is(shopId));
        criteriaList.add(Criteria.where("deleted").ne(true));

        if (StringUtils.hasText(branchId)) {
            criteriaList.add(Criteria.where("branchId").is(branchId));
        }

        if (productIds != null) {
            criteriaList.add(Criteria.where("productId").in(productIds));
        }

        if (req.getActive() != null) {
            criteriaList.add(Criteria.where("activeInBranch").is(req.getActive()));
        }

        if (req.getMinPrice() != null) {
            criteriaList.add(Criteria.where("price").gte(req.getMinPrice()));
        }

        if (req.getMaxPrice() != null) {
            criteriaList.add(Criteria.where("price").lte(req.getMaxPrice()));
        }

        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    /** Map sortBy sang field thực tế trong BranchProduct (chỉ sort được field BranchProduct) */
    private String resolveSortField(String sortBy) {
        return switch (sortBy) {
            case "price" -> "price";
            case "quantity" -> "quantity";
            default -> "createdAt"; // "name", "category" là field của Product, không sort được ở đây
        };
    }
}
