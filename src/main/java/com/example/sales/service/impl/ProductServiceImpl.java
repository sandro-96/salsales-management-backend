// File: src/main/java/com/example/sales/service/impl/ProductServiceImpl.java
package com.example.sales.service.impl;

import com.example.sales.cache.ProductCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.AuditLogService;
import com.example.sales.service.BaseService;
import com.example.sales.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý sản phẩm với mô hình Product và BranchProduct.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends BaseService implements ProductService {
    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository; // New repository
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final ProductCache productCache; // Cache for product operations

    @Override
    public ProductResponse createProduct(String shopId, String branchId, ProductRequest request) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        String sku = StringUtils.hasText(request.getSku())
                ? request.getSku()
                : UUID.randomUUID().toString();

        Optional<Product> existingProduct = productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku);
        Product product;
        if (existingProduct.isPresent()) {
            product = existingProduct.get();
            product.setName(request.getName());
            product.setCategory(request.getCategory());
            productRepository.save(product);
        } else {
            product = Product.builder()
                    .shopId(shopId)
                    .name(request.getName())
                    .category(request.getCategory())
                    .sku(sku)
                    .build();
            product = productRepository.save(product);
        }

        if (branchProductRepository.findByProductIdAndBranchIdAndDeletedFalse(product.getId(), branchId).isPresent()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        BranchProduct branchProduct = BranchProduct.builder()
                .productId(product.getId())
                .shopId(shopId)
                .branchId(branchId)
                .quantity(shop.getType().isTrackInventory() ? request.getQuantity() : 0)
                .price(request.getPrice())
                .unit(request.getUnit())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .activeInBranch(request.isActive()) // Map active from request to activeInBranch
                .build();

        branchProduct = branchProductRepository.save(branchProduct);

        auditLogService.log(null, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "CREATED",
                String.format("Tạo sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        product.getName(), product.getSku(), branchProduct.getBranchId(), branchProduct.getId()));

        return toResponse(branchProduct, product);
    }

    @Override
    public ProductResponse updateProduct(String userId, String shopId, String branchId, String id, ProductRequest request) {
        // id ở đây là id của BranchProduct
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND)); // Should not happen if data is consistent

        // Save old values for audit log from BranchProduct
        double oldPrice = branchProduct.getPrice();
        int oldQuantity = branchProduct.getQuantity();
        boolean oldActiveInBranch = branchProduct.isActiveInBranch();

        // Save old values for audit log from Product
        String oldName = product.getName();
        String oldCategory = product.getCategory();

        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Update Master Product fields (if changed via this request)
        if (!product.getName().equals(request.getName())) {
            product.setName(request.getName());
        }
        if (!product.getCategory().equals(request.getCategory())) {
            product.setCategory(request.getCategory());
        }
        // SKU is usually not updated via product request
        product = productRepository.save(product); // Save master product changes

        // Update BranchProduct fields
        branchProduct.setPrice(request.getPrice());
        branchProduct.setQuantity(shop.getType().isTrackInventory() ? request.getQuantity() : 0);
        branchProduct.setUnit(request.getUnit());
        branchProduct.setImageUrl(request.getImageUrl());
        branchProduct.setDescription(request.getDescription());
        branchProduct.setActiveInBranch(request.isActive()); // Update active status for this branch
        branchProduct.setUpdatedAt(LocalDateTime.now());

        branchProduct = branchProductRepository.save(branchProduct);

        // Audit logs for changes
        if (!oldName.equals(request.getName())) {
            auditLogService.log(userId, shopId, product.getId(), "PRODUCT", "NAME_CHANGED",
                    String.format("Thay đổi tên sản phẩm chung từ '%s' → '%s'", oldName, request.getName()));
        }
        if (!oldCategory.equals(request.getCategory())) {
            auditLogService.log(userId, shopId, product.getId(), "PRODUCT", "CATEGORY_CHANGED",
                    String.format("Thay đổi danh mục sản phẩm chung từ '%s' → '%s'", oldCategory, request.getCategory()));
        }
        if (oldPrice != request.getPrice()) {
            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "PRICE_CHANGED",
                    String.format("Thay đổi giá sản phẩm '%s' từ %.2f → %.2f tại chi nhánh %s", product.getName(), oldPrice, request.getPrice(), branchProduct.getBranchId()));
        }
        if (shop.getType().isTrackInventory() && oldQuantity != request.getQuantity()) {
            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "QUANTITY_CHANGED",
                    String.format("Thay đổi tồn kho sản phẩm '%s' từ %d → %d tại chi nhánh %s", product.getName(), oldQuantity, request.getQuantity(), branchProduct.getBranchId()));
        }
        if (oldActiveInBranch != request.isActive()) {
            String action = request.isActive() ? "ACTIVATED" : "DEACTIVATED";
            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", action,
                    String.format("%s sản phẩm '%s' tại chi nhánh %s",
                            request.isActive() ? "Kích hoạt bán" : "Ngưng bán", product.getName(), branchProduct.getBranchId()));
        }

        return toResponse(branchProduct, product);
    }

    @Override
    public void deleteProduct(String userId, String shopId, String branchId, String id) {
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        branchProduct.setDeleted(true);
        branchProduct.setUpdatedAt(LocalDateTime.now());
        branchProductRepository.save(branchProduct);

        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElse(null); // Master product might already be deleted or not found for some reason

        String productName = (product != null) ? product.getName() : "Unknown Product";
        String productSku = (product != null) ? product.getSku() : "Unknown SKU";

        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "DELETED",
                String.format("Xoá sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        productName, productSku, branchProduct.getBranchId(), branchProduct.getId()));
    }

    @Override
    public ProductResponse getProduct(String shopId, String branchId, String id) {
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        return toResponse(branchProduct, product);
    }

    @Override
    public ProductResponse toggleActive(String userId, String shopId, String branchId, String branchProductId) {
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(branchProductId, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        branchProduct.setActiveInBranch(!branchProduct.isActiveInBranch());
        branchProduct.setUpdatedAt(LocalDateTime.now());
        branchProduct = branchProductRepository.save(branchProduct);

        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElse(null);

        String productName = (product != null) ? product.getName() : "Unknown Product";
        String productSku = (product != null) ? product.getSku() : "Unknown SKU";

        String action = branchProduct.isActiveInBranch() ? "ACTIVATED" : "DEACTIVATED";
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", action,
                String.format("%s sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        branchProduct.isActiveInBranch() ? "Kích hoạt bán" : "Ngưng bán",
                        productName, productSku, branchProduct.getBranchId(), branchProduct.getId()));

        return toResponse(branchProduct, product);
    }

    @Override
    public List<ProductResponse> getLowStockProducts(String shopId, String branchId, int threshold) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        if (!shop.getType().isTrackInventory()) {
            return List.of();
        }

        List<BranchProduct> lowStockBranchProducts;
        if (StringUtils.hasText(branchId)) {
            lowStockBranchProducts = branchProductRepository.findByShopIdAndBranchIdAndQuantityLessThanAndDeletedFalse(shopId, branchId, threshold);
        } else {
            lowStockBranchProducts = branchProductRepository.findByShopIdAndQuantityLessThanAndDeletedFalse(shopId, threshold);
        }

        Set<String> productIds = lowStockBranchProducts.stream()
                .map(BranchProduct::getProductId)
                .collect(Collectors.toSet());

        Map<String, Product> productsMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return lowStockBranchProducts.stream()
                .map(bp -> toResponse(bp, productsMap.get(bp.getProductId())))
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> searchProducts(String shopId, String branchId, String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            // If no keyword, return all products by shop/branch
            return productCache.getAllByShop(shopId, branchId, pageable);
        }

        // 1. Search for matching master Products by keyword
        List<Product> matchedProductsByName = productRepository.findByShopIdAndNameContainingIgnoreCaseAndDeletedFalse(shopId, keyword).stream().toList();
        List<Product> matchedProductsByCategory = productRepository.findByShopIdAndCategoryContainingIgnoreCaseAndDeletedFalse(shopId, keyword).stream().toList();

        // Combine unique product IDs
        Set<String> productIdsFromKeyword = matchedProductsByName.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        matchedProductsByCategory.forEach(p -> productIdsFromKeyword.add(p.getId()));

        if (productIdsFromKeyword.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0); // No master products matched
        }

        // 2. Fetch BranchProducts associated with these master Product IDs, filtering by branchId
        Page<BranchProduct> branchProductsPage;
        if (StringUtils.hasText(branchId)) {
            branchProductsPage = branchProductRepository.findByProductIdInAndShopIdAndBranchIdAndDeletedFalse(productIdsFromKeyword, shopId, branchId, pageable);
        } else {
            branchProductsPage = branchProductRepository.findByProductIdInAndShopIdAndDeletedFalse(productIdsFromKeyword, shopId, pageable);
        }

        // 3. Re-fetch all master Products for the current page of BranchProducts
        // This ensures we have the master product details for response mapping
        Set<String> currentPageProductIds = branchProductsPage.getContent().stream()
                .map(BranchProduct::getProductId)
                .collect(Collectors.toSet());
        Map<String, Product> productsMap = productRepository.findAllById(currentPageProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 4. Map to ProductResponse
        List<ProductResponse> productResponses = branchProductsPage.getContent().stream()
                .map(bp -> toResponse(bp, productsMap.get(bp.getProductId())))
                .collect(Collectors.toList());

        return new PageImpl<>(productResponses, pageable, branchProductsPage.getTotalElements());
    }

    public ProductResponse toResponse(BranchProduct branchProduct, Product product) {
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