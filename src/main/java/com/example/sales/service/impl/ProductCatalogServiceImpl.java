package com.example.sales.service.impl;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.product.ProductCatalogBulkImportResponse;
import com.example.sales.dto.product.ProductCatalogOffBrowseItem;
import com.example.sales.dto.product.ProductCatalogOffBrowsePageResponse;
import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.dto.product.ProductCatalogUpsertRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.integration.openfoodfacts.OpenFoodFactsCatalogMapper;
import com.example.sales.integration.openfoodfacts.OpenFoodFactsClient;
import com.example.sales.model.ProductCatalog;
import com.example.sales.repository.ProductCatalogRepository;
import com.example.sales.service.ProductCatalogService;
import com.example.sales.util.CategoryUtils;
import com.example.sales.util.GtinBarcodeValidator;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private static final String OFF_SOURCE = "open-food-facts";
    private static final String VIETNAM_TAG = "en:vietnam";

    private final ProductCatalogRepository productCatalogRepository;
    private final MongoTemplate mongoTemplate;
    private final OpenFoodFactsClient openFoodFactsClient;

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
    public ProductCatalogOffBrowsePageResponse browseOpenFoodFactsVietnam(int page, int pageSize) {
        JsonNode root = openFoodFactsClient.searchVietnamProducts(page, pageSize)
                .orElseThrow(() -> new BusinessException(ApiCode.OFF_UNAVAILABLE));

        List<ProductCatalogOffBrowseItem> items = new ArrayList<>();
        Set<String> barcodesForLookup = new HashSet<>();
        for (JsonNode product : root.path("products")) {
            ProductCatalogOffBrowseItem item = OpenFoodFactsCatalogMapper.toBrowseItem(product);
            if (item == null) {
                continue;
            }
            try {
                String normalized = GtinBarcodeValidator.resolveForProductSave(item.getBarcode());
                item.setBarcode(normalized);
            } catch (BusinessException ex) {
                continue;
            }
            items.add(item);
            barcodesForLookup.add(item.getBarcode());
            barcodesForLookup.addAll(GtinBarcodeValidator.catalogLookupCandidates(item.getBarcode()));
        }

        if (!barcodesForLookup.isEmpty()) {
            Set<String> existingExpanded = new HashSet<>();
            for (ProductCatalog row : productCatalogRepository.findByBarcodeIn(barcodesForLookup)) {
                existingExpanded.add(row.getBarcode());
                existingExpanded.addAll(GtinBarcodeValidator.catalogLookupCandidates(row.getBarcode()));
            }
            for (ProductCatalogOffBrowseItem item : items) {
                boolean inCatalog = GtinBarcodeValidator.catalogLookupCandidates(item.getBarcode())
                        .stream()
                        .anyMatch(existingExpanded::contains);
                item.setAlreadyInCatalog(inCatalog);
            }
        }

        long total = root.path("count").asLong(items.size());
        return ProductCatalogOffBrowsePageResponse.builder()
                .items(items)
                .page(Math.max(1, page))
                .pageSize(Math.min(Math.max(pageSize, 1), 50))
                .totalCount(total)
                .source(OFF_SOURCE)
                .countryTag(VIETNAM_TAG)
                .build();
    }

    @Override
    public ProductCatalogBulkImportResponse bulkUpsertFromAdmin(List<ProductCatalogUpsertRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ProductCatalogBulkImportResponse.builder()
                    .imported(0)
                    .failed(0)
                    .errors(List.of())
                    .build();
        }
        int imported = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        int maxErrors = 15;
        for (ProductCatalogUpsertRequest req : requests) {
            if (req == null || !StringUtils.hasText(req.getName())) {
                failed++;
                if (errors.size() < maxErrors) {
                    errors.add("Thiếu tên sản phẩm");
                }
                continue;
            }
            try {
                upsertFromAdmin(req);
                imported++;
            } catch (BusinessException ex) {
                failed++;
                if (errors.size() < maxErrors) {
                    String code = req.getBarcode() != null ? req.getBarcode() : "?";
                    errors.add(code + ": " + ex.getMessage());
                }
            } catch (Exception ex) {
                failed++;
                if (errors.size() < maxErrors) {
                    errors.add("Lỗi không xác định");
                }
                log.warn("Bulk catalog import row failed: {}", ex.getMessage());
            }
        }
        return ProductCatalogBulkImportResponse.builder()
                .imported(imported)
                .failed(failed)
                .errors(errors)
                .build();
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
