package com.example.sales.service.impl;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.dto.product.ProductCatalogUpsertRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.ProductCatalog;
import com.example.sales.repository.ProductCatalogRepository;
import com.example.sales.service.ProductCatalogService;
import com.example.sales.util.CategoryUtils;
import com.example.sales.util.GtinBarcodeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
    private final MongoTemplate mongoTemplate;

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

    @Override
    public Page<ProductCatalogResponse> list(String keyword, String category, Pageable pageable) {
        Criteria c = new Criteria();
        if (StringUtils.hasText(keyword)) {
            String q = keyword.trim();
            Pattern p = Pattern.compile(
                    ".*" + Pattern.quote(q) + ".*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            c.orOperator(
                    Criteria.where("name").regex(p),
                    Criteria.where("barcode").regex(p)
            );
        }
        if (StringUtils.hasText(category)) {
            c.and("category").is(CategoryUtils.normalize(category));
        }
        long total = mongoTemplate.count(Query.query(c), ProductCatalog.class);
        List<ProductCatalog> rows = mongoTemplate.find(
                Query.query(c).with(pageable),
                ProductCatalog.class
        );
        List<ProductCatalogResponse> items = rows.stream().map(this::mapToResponse).toList();
        return new PageImpl<>(items, pageable, total);
    }

    @Override
    public void deleteById(String id) {
        ProductCatalog existing = productCatalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        productCatalogRepository.delete(existing);
        log.info("Admin deleted product catalog id={} barcode={}", id, existing.getBarcode());
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
