package com.example.sales.cache;

import com.example.sales.dto.product.ProductResponse;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.ProductRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache layer for product-related data.
 *
 * Key pattern: "branch_products_by_shop_branch" :: "{shopId}:all:p{page}:s{size}:{sort}"
 *                                                   "{shopId}:{branchId}:p{page}:s{size}:{sort}"
 *
 * Paging info được đưa vào key để mỗi trang có entry riêng biệt trong cache.
 * evictByShop(shopId) scan prefix "{shopId}:" để xóa toàn bộ entries của shop.
 */
@Component
public class ProductCache {

    static final String CACHE_NAME = "branch_products_by_shop_branch";

    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CacheManager cacheManager;

    public ProductCache(BranchProductRepository branchProductRepository,
                        ProductRepository productRepository,
                        ProductMapper productMapper,
                        CacheManager cacheManager) {
        this.branchProductRepository = branchProductRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.cacheManager = cacheManager;
    }

    /**
     * Lấy danh sách sản phẩm toàn shop (gộp tất cả chi nhánh).
     * Cache key: "{shopId}:all:p{page}:s{size}:{sort}"
     */
    @Cacheable(value = CACHE_NAME, key = "#shopId + ':all:p' + #pageable.pageNumber + ':s' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ProductResponse> getAllByShop(String shopId, Pageable pageable) {
        Page<BranchProduct> branchProductsPage =
                branchProductRepository.findByShopIdAndDeletedFalse(shopId, pageable);
        return toResponsePage(branchProductsPage, pageable);
    }

    /**
     * Lấy danh sách sản phẩm theo chi nhánh cụ thể.
     * Cache key: "{shopId}:{branchId}:p{page}:s{size}:{sort}"
     */
    @Cacheable(value = CACHE_NAME, key = "#shopId + ':' + #branchId + ':p' + #pageable.pageNumber + ':s' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ProductResponse> getAllByBranch(String shopId, String branchId, Pageable pageable) {
        Page<BranchProduct> branchProductsPage =
                branchProductRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId, pageable);
        return toResponsePage(branchProductsPage, pageable);
    }

    private Page<ProductResponse> toResponsePage(Page<BranchProduct> branchProductsPage, Pageable pageable) {

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
     * Xóa toàn bộ cache entries thuộc về shopId (cả "shopId:all" lẫn "shopId:branchXxx").
     * Gọi sau mọi thao tác create / update / delete product.
     */
    public void evictByShop(String shopId) {
        org.springframework.cache.Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return;

        // Với ConcurrentMapCache (simple) — evict từng key đã biết không đủ,
        // nên kiểm tra nativeCache để scan prefix.
        Object nativeCache = cache.getNativeCache();

        if (nativeCache instanceof java.util.concurrent.ConcurrentMap) {
            // spring.cache.type=simple  →  ConcurrentMapCache
            @SuppressWarnings("unchecked")
            java.util.concurrent.ConcurrentMap<Object, Object> map =
                    (java.util.concurrent.ConcurrentMap<Object, Object>) nativeCache;
            String prefix = shopId + ":";
            map.keySet().removeIf(k -> k.toString().startsWith(prefix));
        } else {
            // spring.cache.type=redis  →  RedisCache  (native = RedisOperations)
            // Dùng allEntries evict sẽ xóa toàn bộ cache; hoặc evict key cụ thể nếu biết.
            // Cách an toàn nhất với Redis là clear toàn bộ cache (ít entry nên chấp nhận được).
            cache.clear();
        }
    }

    /**
     * Update cache entry cho một BranchProduct sau khi cập nhật.
     * Sau đó gọi evictByShop để đảm bảo danh sách cũng được làm mới.
     */
    public void update(String branchProductId, ProductResponse productResponse) {
        // Evict toàn bộ shop thay vì CachePut một key đơn lẻ,
        // vì list cache (getAllByShop) chứa nhiều items, không thể patch từng phần tử.
        evictByShop(productResponse.getShopId());
    }

    /**
     * Evict cache khi xóa một BranchProduct.
     */
    @CacheEvict(value = CACHE_NAME, key = "#shopId + ':' + (#branchId != null ? #branchId : 'all')")
    public void remove(String shopId, String branchId) {
        // handled by annotation — giữ lại để tương thích với code cũ gọi remove(shopId, null)
    }

    /**
     * Clear toàn bộ cache (dùng khi debug hoặc reset).
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clear() {
    }
}