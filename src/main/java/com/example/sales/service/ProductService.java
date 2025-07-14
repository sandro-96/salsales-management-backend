// File: src/main/java/com/example/sales/service/ProductService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.ShopType;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.exception.BusinessException;
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
    private final ShopRepository shopRepository;
    private final ProductSearchHelper productSearchHelper;
    private final ShopUserService shopUserService;
    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;

    public List<ProductResponse> getAllByUser(User user) {
        Shop shop = getShopOfUser(user);
        return productRepository.findByShopId(shop.getId())
                .stream().map(this::toResponse).toList();
    }

    public ProductResponse createProduct(User user, ProductRequest request) {
        Shop shop = getShopOfUser(user);
        shopUserService.requireAnyRole(shop.getId(), user.getId(), ShopRole.OWNER);
        String productCode = request.getProductCode();
        if (productCode == null || productCode.isBlank()) {
            productCode = UUID.randomUUID().toString(); // hoặc custom logic
        }
        String finalImageUrl = fileUploadService.moveToProduct(request.getImageUrl());
        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .unit(request.getUnit())
                .imageUrl(finalImageUrl)
                .description(request.getDescription())
                .active(request.isActive())
                .shopId(shop.getId())
                .productCode(productCode)
                .branchId(request.getBranchId())
                .build();

        if (requiresInventory(shop.getType())) {
            product.setQuantity(request.getQuantity());
        } else {
            product.setQuantity(0);
        }

        return toResponse(productRepository.save(product));
    }

    public ProductResponse updateProduct(User user, String id, ProductRequest request) {
        Shop shop = getShopOfUser(user);
        shopUserService.requireAnyRole(shop.getId(), user.getId(), ShopRole.OWNER);

        Product existing = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

        existing.setName(request.getName());
        existing.setCategory(request.getCategory());
        existing.setPrice(request.getPrice());
        existing.setUnit(request.getUnit());
        existing.setImageUrl(request.getImageUrl());
        existing.setDescription(request.getDescription());
        existing.setActive(request.isActive());

        if (requiresInventory(shop.getType())) {
            existing.setQuantity(request.getQuantity());
        } else {
            existing.setQuantity(0);
        }

        Product updatedProduct = productRepository.save(existing);
        if (existing.getPrice() != request.getPrice()) {
            auditLogService.log(user, shop.getId(), existing.getId(), "PRODUCT", "PRICE_CHANGED",
                    "Thay đổi giá từ %.2f → %.2f".formatted(existing.getPrice(), request.getPrice()));
        }

        if (!existing.getCategory().equals(request.getCategory())) {
            auditLogService.log(user, shop.getId(), existing.getId(), "PRODUCT", "CATEGORY_CHANGED",
                    "Chuyển danh mục từ %s → %s".formatted(existing.getCategory(), request.getCategory()));
        }

        if (existing.getQuantity() != request.getQuantity()) {
            auditLogService.log(user, shop.getId(), existing.getId(), "PRODUCT", "QUANTITY_CHANGED",
                    "Thay đổi tồn kho từ %d → %d".formatted(existing.getQuantity(), request.getQuantity()));
        }

        return toResponse(updatedProduct);
    }

    public void deleteProduct(User user, String id) {
        Shop shop = getShopOfUser(user);
        shopUserService.requireAnyRole(shop.getId(), user.getId(), ShopRole.OWNER);

        Product existing = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

        productRepository.delete(existing);
    }

    public Page<ProductResponse> search(User user, ProductSearchRequest req) {
        Shop shop = getShopOfUser(user);
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());

        List<Product> results = productSearchHelper.search(shop.getId(), req.getBranchId(), req, pageable);
        long total = productSearchHelper.counts(shop.getId(), req.getBranchId(), req);

        return new PageImpl<>(
                results.stream().map(this::toResponse).toList(),
                pageable,
                total
        );
    }

    public List<Product> searchAllForExport(User user, ProductSearchRequest req) {
        Shop shop = getShopOfUser(user);
        return productSearchHelper.search(shop.getId(), req.getBranchId(), req, Pageable.unpaged());
    }

    public ProductResponse toggleActive(User user, String id) {
        Shop shop = getShopOfUser(user);
        shopUserService.requireAnyRole(shop.getId(), user.getId(), ShopRole.OWNER);

        Product product = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

        product.setActive(!product.isActive());
        return toResponse(productRepository.save(product));
    }

    public List<ProductResponse> getLowStock(User user, int threshold) {
        Shop shop = getShopOfUser(user);

        if (!requiresInventory(shop.getType())) {
            return List.of();
        }

        return productRepository.findByShopId(shop.getId()).stream()
                .filter(p -> p.getQuantity() < threshold)
                .map(this::toResponse)
                .toList();
    }

    // ============ Helpers ============

    private Shop getShopOfUser(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
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
                .build();
    }

    private boolean requiresInventory(ShopType type) {
        return switch (type) {
            case GROCERY, CONVENIENCE, PHARMACY, RETAIL -> true;
            case RESTAURANT, CAFE, BAR, OTHER -> false;
        };
    }
}
