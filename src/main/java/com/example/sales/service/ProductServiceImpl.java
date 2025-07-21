// File: src/main/java/com/example/sales/service/ProductServiceImpl.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopType;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý sản phẩm.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends BaseService implements ProductService {
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;

    @Override
    public ProductResponse createProduct(String shopId, ProductRequest request) {
        // Kiểm tra sự tồn tại của cửa hàng
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Kiểm tra branchId nếu có
        if (!StringUtils.hasText(request.getBranchId())) {
            throw new BusinessException(ApiCode.BRANCH_NOT_FOUND);
        }

        // Tạo sku nếu không được cung cấp
        String sku = request.getSku() != null && !request.getSku().isBlank()
                ? request.getSku()
                : UUID.randomUUID().toString();

        // Tạo sản phẩm sử dụng builder pattern
        Product product = Product.builder()
                .shopId(shopId)
                .name(request.getName())
                .price(request.getPrice())
                .quantity(requiresInventory(shop.getType()) ? request.getQuantity() : 0)
                .category(request.getCategory())
                .sku(sku)
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .active(true)
                .unit(request.getUnit())
                .branchId(request.getBranchId())
                .build();

        product = productRepository.save(product);

        // Ghi log kiểm tra
        auditLogService.log(null, shopId, product.getId(), "PRODUCT", "CREATED",
                String.format("Tạo sản phẩm: %s (Mã: %s)", product.getName(), product.getSku()));

        return toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(String userId, String shopId, String id, ProductRequest request) {
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(id, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Lưu các giá trị cũ để ghi log
        double oldPrice = product.getPrice();
        String oldCategory = product.getCategory();
        int oldQuantity = product.getQuantity();

        // Lấy thông tin cửa hàng để kiểm tra ShopType
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Cập nhật các trường
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setQuantity(requiresInventory(shop.getType()) ? request.getQuantity() : 0);
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        product.setUnit(request.getUnit());

        product = productRepository.save(product);

        // Ghi log kiểm tra cho các thay đổi
        if (oldPrice != request.getPrice()) {
            auditLogService.log(userId, shopId, product.getId(), "PRODUCT", "PRICE_CHANGED",
                    String.format("Thay đổi giá từ %.2f → %.2f", oldPrice, request.getPrice()));
        }
        if (!oldCategory.equals(request.getCategory())) {
            auditLogService.log(userId, shopId, product.getId(), "PRODUCT", "CATEGORY_CHANGED",
                    String.format("Chuyển danh mục từ %s → %s", oldCategory, request.getCategory()));
        }
        if (oldQuantity != request.getQuantity()) {
            auditLogService.log(userId, shopId, product.getId(), "PRODUCT", "QUANTITY_CHANGED",
                    String.format("Thay đổi tồn kho từ %d → %d", oldQuantity, request.getQuantity()));
        }

        return toResponse(product);
    }

    @Override
    public void deleteProduct(String shopId, String id) {
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(id, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));
        product.setDeleted(true);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        // Ghi log kiểm tra
        auditLogService.log(null, shopId, product.getId(), "PRODUCT", "DELETED",
                String.format("Xoá sản phẩm: %s (Mã: %s)", product.getName(), product.getSku()));
    }

    @Override
    public ProductResponse getProduct(String shopId, String id) {
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(id, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));
        return toResponse(product);
    }

    @Cacheable(value = "products", key = "#shopId")
    @Override
    public Page<ProductResponse> getAllByShop(String shopId, Pageable pageable) {
        return productRepository.findByShopIdAndDeletedFalse(shopId, pageable)
                .map(this::toResponse);
    }

    @Override
    public ProductResponse toggleActive(String shopId, String productId) {
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(productId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));
        product.setActive(!product.isActive());
        product.setUpdatedAt(LocalDateTime.now());
        product = productRepository.save(product);

        // Ghi log kiểm tra
        String action = product.isActive() ? "ACTIVATED" : "DEACTIVATED";
        auditLogService.log(null, shopId, product.getId(), "PRODUCT", action,
                String.format("%s sản phẩm: %s (Mã: %s)",
                        product.isActive() ? "Kích hoạt" : "Ngưng bán",
                        product.getName(), product.getSku()));

        return toResponse(product);
    }

    @Override
    public List<ProductResponse> getLowStockProducts(String shopId, int threshold) {
        // Kiểm tra loại cửa hàng
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        if (!requiresInventory(shop.getType())) {
            return List.of();
        }

        return productRepository.findByShopIdAndQuantityLessThanAndDeletedFalse(shopId, threshold)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> searchProducts(String shopId, String keyword, Pageable pageable) {
        return productRepository.findByShopIdAndKeyword(shopId, keyword, pageable)
                .map(this::toResponse);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .category(product.getCategory())
                .sku(product.getSku())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .unit(product.getUnit())
                .build();
    }

    private boolean requiresInventory(ShopType type) {
        return switch (type) {
            case GROCERY, CONVENIENCE, PHARMACY, RETAIL -> true;
            case RESTAURANT, CAFE, BAR, OTHER -> false;
        };
    }
}