// File: src/main/java/com/example/sales/cache/ProductCache.java
package com.example.sales.cache;

import com.example.sales.dto.product.ProductResponse;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.ProductRepository;
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

@Component
public class ProductCache {

    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;

    public ProductCache(BranchProductRepository branchProductRepository,
                        ProductRepository productRepository) {
        this.branchProductRepository = branchProductRepository;
        this.productRepository = productRepository;
    }

    @Cacheable(value = "branch_products_by_shop_branch", key = "#shopId + ':' + #branchId")
    public Page<ProductResponse> getAllByShop(String shopId, String branchId, Pageable pageable) {
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
                .map(bp -> toResponse(bp, productsMap.get(bp.getProductId())))
                .collect(Collectors.toList());

        return new PageImpl<>(productResponses, pageable, branchProductsPage.getTotalElements());
    }
    private ProductResponse toResponse(BranchProduct branchProduct, Product product) {
        if (branchProduct == null || product == null) {
            return null; // Handle cases where product might be null (e.g., deleted master product)
        }
        return ProductResponse.builder()
                .id(branchProduct.getId()) // ID của BranchProduct
                .productId(product.getId()) // ID của Master Product
                .name(product.getName())
                .category(product.getCategory())
                .sku(product.getSku())
                .quantity(branchProduct.getQuantity())
                .price(branchProduct.getPrice())
                .unit(branchProduct.getUnit())
                .imageUrl(branchProduct.getImageUrl())
                .description(branchProduct.getDescription())
                .branchId(branchProduct.getBranchId())
                .activeInBranch(branchProduct.isActiveInBranch())
                .createdAt(branchProduct.getCreatedAt()) // createdAt/updatedAt của BranchProduct
                .updatedAt(branchProduct.getUpdatedAt())
                .build();
    }
}
