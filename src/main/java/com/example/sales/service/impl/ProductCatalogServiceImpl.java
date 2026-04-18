package com.example.sales.service.impl;

import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.dto.product.ProductCatalogUpsertRequest;
import com.example.sales.model.ProductCatalog;
import com.example.sales.repository.ProductCatalogRepository;
import com.example.sales.service.ProductCatalogService;
import com.example.sales.util.CategoryUtils;
import com.example.sales.util.GtinBarcodeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductCatalogRepository productCatalogRepository;

    @Override
    public ProductCatalogResponse upsertFromAdmin(ProductCatalogUpsertRequest request) {
        String barcode = GtinBarcodeValidator.resolveForProductSave(request.getBarcode());

        ProductCatalog catalog = productCatalogRepository.findByBarcode(barcode)
                .orElse(ProductCatalog.builder().barcode(barcode).build());

        catalog.setName(request.getName().trim());
        if (StringUtils.hasText(request.getCategory())) {
            catalog.setCategory(CategoryUtils.normalize(request.getCategory()));
        } else {
            catalog.setCategory(null);
        }
        catalog.setDescription(request.getDescription());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            catalog.setImages(request.getImages());
        }

        catalog = productCatalogRepository.save(catalog);
        log.debug("Admin saved product catalog for barcode: {}", barcode);
        return mapToResponse(catalog);
    }

    @Override
    public List<ProductCatalogResponse> searchByNameKeyword(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String q = keyword.trim();
        if (q.length() < 2) {
            return List.of();
        }
        if (q.length() > 200) {
            q = q.substring(0, 200);
        }
        int cap = Math.min(Math.max(limit, 1), 50);
        Pattern pattern = Pattern.compile(
                ".*" + Pattern.quote(q) + ".*",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        return productCatalogRepository.findByNameRegex(pattern, PageRequest.of(0, cap))
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Optional<ProductCatalogResponse> findByBarcode(String barcode) {
        for (String candidate : GtinBarcodeValidator.catalogLookupCandidates(barcode)) {
            Optional<ProductCatalogResponse> hit = productCatalogRepository.findByBarcode(candidate)
                    .map(this::mapToResponse);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
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
