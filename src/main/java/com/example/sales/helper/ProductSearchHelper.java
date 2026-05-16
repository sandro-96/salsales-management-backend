// File: src/main/java/com/example/sales/helper/ProductSearchHelper.java
package com.example.sales.helper;

import com.example.sales.dto.inventory.InventorySummaryResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.bson.Document;
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
    private final ProductRepository productRepository;

    private static final String STOCK_ALL = "ALL";
    private static final String STOCK_IN = "IN_STOCK";
    private static final String STOCK_LOW = "LOW_STOCK";
    private static final String STOCK_OUT = "OUT_OF_STOCK";
    private static final String STOCK_NOT_TRACKED = "NOT_TRACKED";

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

        if (req.getActive() != null) {
            criteriaList.add(Criteria.where("active").is(req.getActive()));
        }

        applyProductLevelStockStatus(criteriaList, req.getStockStatus());

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

        applyBranchLevelStockStatus(criteriaList, req.getStockStatus());

        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    private void applyProductLevelStockStatus(List<Criteria> criteriaList, String stockStatus) {
        if (!StringUtils.hasText(stockStatus) || STOCK_ALL.equalsIgnoreCase(stockStatus)) {
            return;
        }
        if (STOCK_NOT_TRACKED.equalsIgnoreCase(stockStatus)) {
            criteriaList.add(Criteria.where("trackInventory").is(false));
            return;
        }
        if (STOCK_IN.equalsIgnoreCase(stockStatus)
                || STOCK_LOW.equalsIgnoreCase(stockStatus)
                || STOCK_OUT.equalsIgnoreCase(stockStatus)) {
            criteriaList.add(Criteria.where("trackInventory").is(true));
        }
    }

    private void applyBranchLevelStockStatus(List<Criteria> criteriaList, String stockStatus) {
        if (!StringUtils.hasText(stockStatus) || STOCK_ALL.equalsIgnoreCase(stockStatus)
                || STOCK_NOT_TRACKED.equalsIgnoreCase(stockStatus)) {
            return;
        }
        if (STOCK_OUT.equalsIgnoreCase(stockStatus)) {
            criteriaList.add(Criteria.where("quantity").lte(0));
            return;
        }
        if (STOCK_LOW.equalsIgnoreCase(stockStatus)) {
            criteriaList.add(Criteria.where("quantity").gt(0));
            criteriaList.add(Criteria.where("minQuantity").gt(0));
            criteriaList.add(Criteria.where("$expr").is(
                    new Document("$lte", Arrays.asList("$quantity", "$minQuantity"))));
            return;
        }
        if (STOCK_IN.equalsIgnoreCase(stockStatus)) {
            criteriaList.add(new Criteria().orOperator(
                    new Criteria().andOperator(
                            Criteria.where("quantity").gt(0),
                            Criteria.where("minQuantity").lte(0)),
                    Criteria.where("$expr").is(
                            new Document("$gt", Arrays.asList("$quantity", "$minQuantity")))));
        }
    }

    /**
     * Thống kê tồn kho toàn chi nhánh (không phụ thuộc trang hiện tại).
     */
    public InventorySummaryResponse computeBranchInventorySummary(
            String shopId, String branchId, String keyword) {
        ProductSearchRequest req = new ProductSearchRequest();
        req.setKeyword(keyword != null ? keyword : "");
        req.setStockStatus(STOCK_ALL);

        Set<String> productIds = findMatchingProductIds(shopId, req);
        if (productIds != null && productIds.isEmpty()) {
            return new InventorySummaryResponse(0, 0, 0, 0, 0);
        }

        Query query = new Query(buildBranchCriteria(shopId, branchId, productIds, req));
        List<BranchProduct> branchProducts =
                mongoTemplate.find(query, BranchProduct.class, "branch_products");

        Set<String> ids = branchProducts.stream()
                .map(BranchProduct::getProductId)
                .collect(Collectors.toSet());
        Map<String, Product> productsMap = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        long tracked = 0;
        long totalStock = 0;
        long lowStock = 0;
        long outOfStock = 0;
        long notTracked = 0;

        for (BranchProduct bp : branchProducts) {
            Product product = productsMap.get(bp.getProductId());
            if (product == null || product.isDeleted()) {
                continue;
            }
            if (!product.isTrackInventory()) {
                notTracked++;
                continue;
            }
            tracked++;
            int qty = bp.getQuantity();
            totalStock += Math.max(0, qty);
            if (qty <= 0) {
                outOfStock++;
            } else if (bp.getMinQuantity() > 0 && qty <= bp.getMinQuantity()) {
                lowStock++;
            }
        }

        return new InventorySummaryResponse(tracked, totalStock, lowStock, outOfStock, notTracked);
    }

    /**
     * Danh sách / đếm sản phẩm cấp shop (collection products) — dùng cho trang quản lý SP.
     */
    public Criteria buildProductLevelCriteria(
            String shopId, String keyword, String category, Boolean active) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("shopId").is(shopId));
        criteriaList.add(Criteria.where("deleted").ne(true));

        if (StringUtils.hasText(keyword)) {
            String pattern = ".*" + keyword.trim() + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern, "i"),
                    Criteria.where("sku").regex(pattern, "i"),
                    Criteria.where("barcode").regex(pattern, "i")));
        }
        if (StringUtils.hasText(category)) {
            criteriaList.add(Criteria.where("category").is(category));
        }
        if (active != null) {
            criteriaList.add(Criteria.where("active").is(active));
        }
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    public long countProductsAtShopLevel(
            String shopId, String keyword, String category, Boolean active) {
        Query query = new Query(buildProductLevelCriteria(shopId, keyword, category, active));
        return mongoTemplate.count(query, Product.class);
    }

    public Page<Product> findProductsAtShopLevel(
            String shopId, String keyword, String category, Boolean active, Pageable pageable) {
        Criteria criteria = buildProductLevelCriteria(shopId, keyword, category, active);
        Query countQuery = new Query(criteria);
        long total = mongoTemplate.count(countQuery, Product.class);

        Query query = new Query(criteria).with(pageable);
        List<Product> content = mongoTemplate.find(query, Product.class);
        return new PageImpl<>(content, pageable, total);
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
