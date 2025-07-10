package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.ShopType;
import com.example.sales.dto.ProductRequest;
import com.example.sales.dto.ProductResponse;
import com.example.sales.dto.ProductSearchRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.helper.ProductSearchHelper;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ProductSearchHelper productSearchHelper;

    public List<ProductResponse> getAllByUser(User user) {
        Shop shop = getShopOfUser(user);
        return productRepository.findByShopId(shop.getId())
                .stream().map(this::toResponse).toList();
    }

    public ProductResponse createProduct(User user, ProductRequest request) {
        Shop shop = getShopOfUser(user);

        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .unit(request.getUnit())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .active(request.isActive())
                .shopId(shop.getId())
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

        Product existing = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

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

        return toResponse(productRepository.save(existing));
    }

    public void deleteProduct(User user, String id) {
        Shop shop = getShopOfUser(user);

        Product existing = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

        productRepository.delete(existing);
    }

    public Page<ProductResponse> search(User user, ProductSearchRequest req) {
        Shop shop = getShopOfUser(user);
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());

        List<Product> results = productSearchHelper.search(shop.getId(), req, pageable);
        long total = productSearchHelper.counts(shop.getId(), req);

        return new PageImpl<>(
                results.stream().map(this::toResponse).toList(),
                pageable,
                total
        );
    }

    public List<Product> searchAllForExport(User user, ProductSearchRequest req) {
        Shop shop = getShopOfUser(user);
        return productSearchHelper.search(shop.getId(), req, Pageable.unpaged());
    }

    public ProductResponse toggleActive(User user, String id) {
        Shop shop = getShopOfUser(user);

        Product product = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));
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
                .build();
    }

    private boolean requiresInventory(ShopType type) {
        return switch (type) {
            case GROCERY, CONVENIENCE, PHARMACY, RETAIL -> true;
            case RESTAURANT, CAFE, BAR, OTHER -> false;
        };
    }
}
