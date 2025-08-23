package com.example.sales.cache;

import com.example.sales.dto.product.ProductResponse;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache layer for product-related data, handling caching of ProductResponse lists and individual items.
 * Supports create/update/delete operations to keep cache in sync with database.
 */
@Component
public class ProductCache {

    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductCache(BranchProductRepository branchProductRepository,
                        ProductRepository productRepository,
                        ProductMapper productMapper) {
        this.branchProductRepository = branchProductRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    /**
     * Get paginated list of products for a shop, optionally filtered by branch.
     * Cache result based on shopId and branchId (if provided).
     */
    @Cacheable(value = "branch_products_by_shop_branch", key = "#shopId + ':' + (#branchId != null ? #branchId : 'all')")
    public Page<ProductResponse> getAllByShop(String shopId, String branchId, Pageable pageable) {
        String cacheKey = shopId + ":" + (branchId != null ? branchId : "all");
        System.out.println("Generated cache key: branch_products_by_shop_branch:" + cacheKey);
        Page<BranchProduct> branchProductsPage;
        if (StringUtils.hasText(branchId)) {
            branchProductsPage = branchProductRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId, pageable);
        } else {
            branchProductsPage = branchProductRepository.findByShopIdAndDeletedFalse(shopId, pageable);
        }

        Set<String> productIds = branchProductsPage.getContent().stream()
                .map(BranchProduct::getProductId)
                .collect(Collectors.toSet());

        Map<String, Product> productsMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ProductResponse> productResponses = branchProductsPage.getContent().stream()
                .map(bp -> productMapper.toResponse(bp, productsMap.get(bp.getProductId())))
                .collect(Collectors.toList());

        return new PageImpl<>(productResponses, pageable, branchProductsPage.getTotalElements());
    }

    /**
     * Update cache with new ProductResponse for a specific branchProductId.
     */
    @CachePut(value = "branch_products_by_shop_branch", key = "#result.branchId != null ? #result.shopId + ':' + #result.branchId : #result.shopId + ':all'")
    public ProductResponse update(String branchProductId, ProductResponse productResponse) {
        return productResponse;
    }

    /**
     * Remove product from cache for a specific branchProductId.
     * Invalidates cache for the corresponding shopId:branchId key.
     */
    @CacheEvict(value = "branch_products_by_shop_branch", key = "#result != null ? #result.shopId + ':' + (#result.branchId != null ? #result.branchId : 'all') : 'unknown'")
    public BranchProduct remove(String branchProductId) {
        BranchProduct branchProduct = branchProductRepository.findByIdAndDeletedFalse(branchProductId)
                .orElse(null);
        return branchProduct; // Return BranchProduct to generate correct cache key
    }

    /**
     * Remove product from cache for a specific shopId and branchId.
     * Invalidates entire cache for shopId:branchId or shopId:all.
     */
    @CacheEvict(value = "branch_products_by_shop_branch", key = "#shopId + ':' + (#branchId != null ? #branchId : 'all')")
    public void remove(String shopId, String branchId) {
        // Cache eviction handled by annotation
    }

    /**
     * Clear all cache entries manually (useful for debugging or cache reset in local dev).
     */
    @CacheEvict(value = "branch_products_by_shop_branch", allEntries = true)
    public void clear() {
        // Clear all Redis cache entries, handled by annotation
    }
}