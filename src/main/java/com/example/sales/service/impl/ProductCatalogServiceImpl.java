package com.example.sales.service.impl;

import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.model.ProductCatalog;
import com.example.sales.repository.ProductCatalogRepository;
import com.example.sales.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductCatalogRepository productCatalogRepository;

    /**
     * Upsert catalog — chạy async để không block luồng tạo/cập nhật sản phẩm.
     * Nếu upsert lỗi (vd: duplicate key race condition) chỉ log warning, không throw.
     */
    @Override
    @Async
    public void upsert(String barcode, String name, String category, String description, List<String> images) {
        if (!StringUtils.hasText(barcode)) return;

        try {
            ProductCatalog catalog = productCatalogRepository.findByBarcode(barcode)
                    .orElse(ProductCatalog.builder().barcode(barcode).build());

            catalog.setName(name);
            catalog.setCategory(category);
            catalog.setDescription(description);
            // Chỉ overwrite images nếu request có ảnh (tránh xóa ảnh cũ khi shop khác update text only)
            if (images != null && !images.isEmpty()) {
                catalog.setImages(images);
            }

            productCatalogRepository.save(catalog);
            log.debug("Upserted product catalog for barcode: {}", barcode);
        } catch (Exception e) {
            // Không throw — catalog là tính năng phụ trợ, không được làm fail luồng chính
            log.warn("Failed to upsert product catalog for barcode '{}': {}", barcode, e.getMessage());
        }
    }

    @Override
    public Optional<ProductCatalogResponse> findByBarcode(String barcode) {
        return productCatalogRepository.findByBarcode(barcode)
                .map(this::mapToResponse);
    }

    private ProductCatalogResponse mapToResponse(ProductCatalog catalog) {
        return ProductCatalogResponse.builder()
                .id(catalog.getId())
                .barcode(catalog.getBarcode())
                .name(catalog.getName())
                .category(catalog.getCategory())
                .description(catalog.getDescription())
                .images(catalog.getImages())
                .createdAt(catalog.getCreatedAt())
                .updatedAt(catalog.getUpdatedAt())
                .build();
    }
}

