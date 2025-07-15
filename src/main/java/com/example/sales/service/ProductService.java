// File: src/main/java/com/example/sales/service/ProductService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopType;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.helper.ProductSearchHelper;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchHelper productSearchHelper;
    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;
    private final ShopRepository shopRepository;

    public List<ProductResponse> getAllByShop(String shopId) {
        return productRepository.findByShopIdAndDeletedFalse(shopId)
                .stream().map(this::toResponse).toList();
    }

    public ProductResponse createProduct(String shopId, ProductRequest request) {
        String productCode = request.getProductCode();
        if (productCode == null || productCode.isBlank()) {
            productCode = UUID.randomUUID().toString();
        }
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));
        String finalImageUrl = fileUploadService.moveToProduct(request.getImageUrl());
        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .unit(request.getUnit())
                .imageUrl(finalImageUrl)
                .description(request.getDescription())
                .active(request.isActive())
                .shopId(shopId)
                .productCode(productCode)
                .branchId(request.getBranchId())
                .build();

        product.setQuantity(requiresInventory(shop.getType()) ? request.getQuantity() : 0);

        Product saved = productRepository.save(product);
        auditLogService.log(null, shopId, saved.getId(), "PRODUCT", "CREATED",
                String.format("Tạo sản phẩm: %s (Mã: %s)", saved.getName(), saved.getProductCode()));
        return toResponse(saved);
    }

    public ProductResponse updateProduct(User user, String shopId, ShopType shopType, String id, ProductRequest request) {
        Product existing = productRepository.findByIdAndDeletedFalse(id)
                .filter(p -> p.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

        existing.setName(request.getName());
        existing.setCategory(request.getCategory());
        existing.setPrice(request.getPrice());
        existing.setUnit(request.getUnit());
        existing.setImageUrl(request.getImageUrl());
        existing.setDescription(request.getDescription());
        existing.setActive(request.isActive());
        existing.setQuantity(requiresInventory(shopType) ? request.getQuantity() : 0);

        Product updatedProduct = productRepository.save(existing);

        if (existing.getPrice() != request.getPrice()) {
            auditLogService.log(user, shopId, existing.getId(), "PRODUCT", "PRICE_CHANGED",
                    "Thay đổi giá từ %.2f → %.2f".formatted(existing.getPrice(), request.getPrice()));
        }

        if (!existing.getCategory().equals(request.getCategory())) {
            auditLogService.log(user, shopId, existing.getId(), "PRODUCT", "CATEGORY_CHANGED",
                    "Chuyển danh mục từ %s → %s".formatted(existing.getCategory(), request.getCategory()));
        }

        if (existing.getQuantity() != request.getQuantity()) {
            auditLogService.log(user, shopId, existing.getId(), "PRODUCT", "QUANTITY_CHANGED",
                    "Thay đổi tồn kho từ %d → %d".formatted(existing.getQuantity(), request.getQuantity()));
        }

        return toResponse(updatedProduct);
    }

    public void deleteProduct(String shopId, String id) {
        Product existing = productRepository.findByIdAndDeletedFalse(id)
                .filter(p -> p.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));
        existing.setDeleted(true);
        productRepository.save(existing);
        auditLogService.log(null, shopId, existing.getId(), "PRODUCT", "DELETED",
                String.format("Xoá sản phẩm: %s (Mã: %s)", existing.getName(), existing.getProductCode()));
    }

    public Page<ProductResponse> search(String shopId, ProductSearchRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());
        List<Product> results = productSearchHelper.search(shopId, req.getBranchId(), req, pageable);
        long total = productSearchHelper.counts(shopId, req.getBranchId(), req);

        return new PageImpl<>(
                results.stream().map(this::toResponse).toList(),
                pageable,
                total
        );
    }

    public List<Product> searchAllForExport(String shopId, ProductSearchRequest req) {
        return productSearchHelper.search(shopId, req.getBranchId(), req, Pageable.unpaged());
    }

    public ProductResponse toggleActive(String shopId, String id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .filter(p -> p.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

        product.setActive(!product.isActive());
        Product updated = productRepository.save(product);
        String action = product.isActive() ? "ACTIVATED" : "DEACTIVATED";
        auditLogService.log(null, shopId, product.getId(), "PRODUCT", action,
                String.format("%s sản phẩm: %s (Mã: %s)", product.isActive() ? "Kích hoạt" : "Ngưng bán",
                        product.getName(), product.getProductCode()));
        return toResponse(updated);
    }

    public List<ProductResponse> getLowStock(String shopId, int threshold, ShopType shopType) {
        if (!requiresInventory(shopType)) {
            return List.of();
        }

        return productRepository.findByShopIdAndDeletedFalse(shopId).stream()
                .filter(p -> p.getQuantity() < threshold)
                .map(this::toResponse)
                .toList();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .quantity(product.getQuantity())
                .price(product.getPrice())
                .unit(product.getUnit())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .active(product.isActive())
                .productCode(product.getProductCode())
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().toString() : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().toString() : null)
                .shopId(product.getShopId())
                .build();
    }

    private boolean requiresInventory(ShopType type) {
        return switch (type) {
            case GROCERY, CONVENIENCE, PHARMACY, RETAIL -> true;
            case RESTAURANT, CAFE, BAR, OTHER -> false;
        };
    }
}
