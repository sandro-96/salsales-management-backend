package com.example.sales.service.impl;

import com.example.sales.cache.ProductCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.AppConstants;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Branch;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.AuditLogService;
import com.example.sales.service.BaseService;
import com.example.sales.service.ProductService;
import com.example.sales.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý sản phẩm với mô hình Product (thông tin chung) và BranchProduct (thông tin đặc thù tại chi nhánh).
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends BaseService implements ProductService {
    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;
    private final ProductCache productCache;
    private final SequenceService sequenceService;

    @Override
    public ProductResponse createProduct(String shopId, List<String> branchIds, ProductRequest request) {
        // Validate shop
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Validate and fetch branches in one query
        Map<String, Branch> validBranches = new HashMap<>();
        if (branchIds != null && !branchIds.isEmpty()) {
            List<Branch> branches = branchRepository.findAllByShopIdAndDeletedFalse(shopId);
            validBranches = branches.stream()
                    .collect(Collectors.toMap(Branch::getId, branch -> branch));
            for (String branchId : branchIds) {
                if (StringUtils.hasText(branchId) && !validBranches.containsKey(branchId)) {
                    throw new BusinessException(ApiCode.BRANCH_NOT_FOUND);
                }
            }
        }

        // Validate barcode uniqueness (if provided)
        if (StringUtils.hasText(request.getBarcode())) {
            productRepository.findByShopIdAndBarcodeAndDeletedFalse(shopId, request.getBarcode())
                    .ifPresent(product -> {
                        throw new BusinessException(ApiCode.BARCODE_EXISTS);
                    });
        }

        // Generate SKU prefix
        String prefix = StringUtils.hasText(request.getCategory())
                ? String.format("%s_%s", shop.getType().getIndustry().name().toUpperCase(), request.getCategory().toUpperCase())
                : shop.getType().getIndustry().name().toUpperCase();

        // Generate or use provided SKU
        String sku = StringUtils.hasText(request.getSku())
                ? request.getSku()
                : sequenceService.getNextCode(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);

        // Create or update product
        Product product = productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku)
                .map(existing -> updateExistingProduct(existing, request))
                .orElseGet(() -> createNewProduct(shopId, sku, request));

        product = productRepository.save(product);
        if (!productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku).isPresent()) {
            sequenceService.updateNextSequence(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
        }

        // Handle branch products
        List<BranchProduct> branchProducts = new ArrayList<>();
        if (branchIds != null && !branchIds.isEmpty()) {
            // Check for existing BranchProduct
            List<BranchProduct> existingBranchProducts = branchProductRepository
                    .findByProductIdAndBranchIdInAndDeletedFalse(product.getId(), branchIds);
            Set<String> existingBranchIds = existingBranchProducts.stream()
                    .map(BranchProduct::getBranchId)
                    .collect(Collectors.toSet());
            if (!existingBranchIds.isEmpty()) {
                throw new BusinessException(ApiCode.PRODUCT_EXISTS_IN_BRANCH);
            }

            // Create BranchProduct list
            for (String branchId : branchIds) {
                if (StringUtils.hasText(branchId)) {
                    BranchProduct branchProduct = BranchProduct.builder()
                            .productId(product.getId())
                            .shopId(shopId)
                            .branchId(branchId)
                            .quantity(shop.getType().isTrackInventory() ? request.getQuantity() : 0)
                            .minQuantity(request.getMinQuantity())
                            .price(request.getPrice())
                            .branchCostPrice(request.getBranchCostPrice())
                            .discountPrice(request.getDiscountPrice())
                            .discountPercentage(request.getDiscountPercentage())
                            .expiryDate(request.getExpiryDate())
                            .variants(request.getBranchVariants())
                            .activeInBranch(request.isActive())
                            .product(product)
                            .shop(shop)
                            .branch(validBranches.get(branchId))
                            .build();
                    branchProducts.add(branchProduct);
                }
            }

            // Batch save BranchProduct
            branchProducts = branchProductRepository.saveAll(branchProducts);

            // Log audit for all branches in one entry
            String branchIdsStr = branchIds.stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            auditLogService.log(null, shopId, product.getId(), "PRODUCT", "CREATED",
                    String.format("Tạo sản phẩm '%s' (SKU: %s) tại các chi nhánh: %s",
                            product.getName(), product.getSku(), branchIdsStr));
        }

        // Return response (using first BranchProduct or product if no branches)
        return toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    private Product updateExistingProduct(Product existing, ProductRequest request) {
        existing.setName(request.getName());
        existing.setNameTranslations(request.getNameTranslations());
        existing.setCategory(request.getCategory());
        existing.setCostPrice(request.getCostPrice());
        existing.setDefaultPrice(request.getDefaultPrice());
        existing.setUnit(request.getUnit());
        existing.setDescription(request.getDescription());
        existing.setImages(request.getImages());
        existing.setBarcode(request.getBarcode());
        existing.setSupplierId(request.getSupplierId());
        existing.setVariants(request.getVariants());
        existing.setPriceHistory(request.getPriceHistory());
        existing.setActive(request.isActive());
        return existing;
    }

    private Product createNewProduct(String shopId, String sku, ProductRequest request) {
        return Product.builder()
                .shopId(shopId)
                .name(request.getName())
                .nameTranslations(request.getNameTranslations())
                .category(request.getCategory())
                .sku(sku)
                .costPrice(request.getCostPrice())
                .defaultPrice(request.getDefaultPrice())
                .unit(request.getUnit())
                .description(request.getDescription())
                .images(request.getImages())
                .barcode(request.getBarcode())
                .supplierId(request.getSupplierId())
                .variants(request.getVariants())
                .priceHistory(request.getPriceHistory())
                .active(request.isActive())
                .build();
    }

    @Override
    public ProductResponse updateProduct(String userId, String shopId, List<String> branchIds, String id, ProductRequest request) {
        // Validate shop
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Validate and fetch branches in one query
        Map<String, Branch> validBranches = new HashMap<>();
        if (branchIds != null && !branchIds.isEmpty()) {
            List<Branch> branches = branchRepository.findAllByShopIdAndDeletedFalse(shopId);
            validBranches = branches.stream()
                    .collect(Collectors.toMap(Branch::getId, branch -> branch));
            for (String branchId : branchIds) {
                if (StringUtils.hasText(branchId) && !validBranches.containsKey(branchId)) {
                    throw new BusinessException(ApiCode.BRANCH_NOT_FOUND);
                }
            }
        }

        // Validate barcode uniqueness (if provided and changed)
        if (StringUtils.hasText(request.getBarcode())) {
            productRepository.findByShopIdAndBarcodeAndDeletedFalse(shopId, request.getBarcode())
                    .ifPresent(product -> {
                        if (!product.getId().equals(id)) {
                            throw new BusinessException(ApiCode.BARCODE_EXISTS);
                        }
                    });
        }

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(id, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Save old values for audit log
        String oldName = product.getName();
        String oldCategory = product.getCategory();
        String oldBarcode = product.getBarcode();

        // Update Product
        product.setName(request.getName());
        product.setNameTranslations(request.getNameTranslations());
        product.setCategory(request.getCategory());
        product.setCostPrice(request.getCostPrice());
        product.setDefaultPrice(request.getDefaultPrice());
        product.setUnit(request.getUnit());
        product.setDescription(request.getDescription());
        product.setImages(request.getImages());
        product.setBarcode(request.getBarcode());
        product.setSupplierId(request.getSupplierId());
        product.setVariants(request.getVariants());
        product.setPriceHistory(request.getPriceHistory());
        product.setActive(request.isActive());
        product = productRepository.save(product);

        // Handle BranchProduct updates
        List<BranchProduct> branchProducts = new ArrayList<>();
        List<String> updatedBranchIds = new ArrayList<>();
        if (branchIds != null && !branchIds.isEmpty()) {
            // Fetch all existing BranchProduct for the product in one query
            List<BranchProduct> existingBranchProducts = branchProductRepository
                    .findByProductIdAndBranchIdInAndDeletedFalse(id, branchIds);
            Map<String, BranchProduct> branchProductMap = existingBranchProducts.stream()
                    .collect(Collectors.toMap(BranchProduct::getBranchId, bp -> bp));

            // Update or create BranchProduct for each branchId
            for (String branchId : branchIds) {
                if (StringUtils.hasText(branchId)) {
                    BranchProduct branchProduct = branchProductMap.getOrDefault(branchId, BranchProduct.builder()
                            .productId(id)
                            .shopId(shopId)
                            .branchId(branchId)
                            .product(product)
                            .shop(shop)
                            .branch(validBranches.get(branchId))
                            .build());

                    // Save old values for audit log
                    double oldPrice = branchProduct.getPrice();
                    int oldQuantity = branchProduct.getQuantity();
                    boolean oldActiveInBranch = branchProduct.isActiveInBranch();
                    double oldBranchCostPrice = branchProduct.getBranchCostPrice();
                    Double oldDiscountPrice = branchProduct.getDiscountPrice();
                    Double oldDiscountPercentage = branchProduct.getDiscountPercentage();

                    // Update BranchProduct fields
                    branchProduct.setPrice(request.getPrice());
                    branchProduct.setQuantity(shop.getType().isTrackInventory() ? request.getQuantity() : 0);
                    branchProduct.setMinQuantity(request.getMinQuantity());
                    branchProduct.setBranchCostPrice(request.getBranchCostPrice());
                    branchProduct.setDiscountPrice(request.getDiscountPrice());
                    branchProduct.setDiscountPercentage(request.getDiscountPercentage());
                    branchProduct.setExpiryDate(request.getExpiryDate());
                    branchProduct.setVariants(request.getBranchVariants());
                    branchProduct.setActiveInBranch(request.isActive());
                    branchProducts.add(branchProduct);

                    // Log changes for this BranchProduct
                    if (branchProductMap.containsKey(branchId)) {
                        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "UPDATED",
                                String.format("Cập nhật sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s. " +
                                                "Thông tin cũ - Tên: %s, Danh mục: %s, Giá: %.2f, Số lượng: %d, Kích hoạt: %b, Giá nhập: %.2f, Giảm giá: %.2f%%",
                                        product.getName(), product.getSku(), branchId, branchProduct.getId(),
                                        oldName, oldCategory, oldPrice, oldQuantity, oldActiveInBranch,
                                        oldBranchCostPrice, oldDiscountPercentage != null ? oldDiscountPercentage : 0.0));
                    } else {
                        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "CREATED",
                                String.format("Tạo mới BranchProduct cho sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                                        product.getName(), product.getSku(), branchId, branchProduct.getId()));
                    }
                    updatedBranchIds.add(branchId);
                }
            }

            // Async save BranchProduct
            branchProducts = saveAllBranchProducts(branchProducts).join();
        }

        // Log summary for all branches
        if (!updatedBranchIds.isEmpty()) {
            String branchIdsStr = updatedBranchIds.stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            auditLogService.log(userId, shopId, id, "PRODUCT", "UPDATED",
                    String.format("Cập nhật sản phẩm '%s' (SKU: %s) tại các chi nhánh: %s",
                            product.getName(), product.getSku(), branchIdsStr));
        }

        // Return response (using first BranchProduct or product if no branches)
        return toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Async("taskExecutor")
    public CompletableFuture<List<BranchProduct>> saveAllBranchProducts(List<BranchProduct> branchProducts) {
        return CompletableFuture.completedFuture(branchProductRepository.saveAll(branchProducts));
    }

    @Override
    public void deleteProduct(String userId, String shopId, String branchId, String id) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        branchProduct.setDeleted(true);
        branchProductRepository.save(branchProduct);

        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "DELETED",
                String.format("Xóa sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        product.getName(), product.getSku(), branchId, branchProduct.getId()));
    }

    @Override
    public ProductResponse getProduct(String shopId, String branchId, String id) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        return toResponse(branchProduct, product);
    }

    @Override
    public ProductResponse toggleActive(String userId, String shopId, String branchId, String branchProductId) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(branchProductId, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        branchProduct.setActiveInBranch(!branchProduct.isActiveInBranch());
        branchProduct = branchProductRepository.save(branchProduct);

        String action = branchProduct.isActiveInBranch() ? "ACTIVATED" : "DEACTIVATED";
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", action,
                String.format("%s sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        branchProduct.isActiveInBranch() ? "Kích hoạt bán" : "Ngưng bán",
                        product.getName(), product.getSku(), branchId, branchProduct.getId()));

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
            branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                    .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));
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
    public String getSuggestedSku(String shopId, String industry, String category) {
        String prefix = StringUtils.hasText(category)
                ? String.format("%s_%s", industry.toUpperCase(), category.toUpperCase())
                : industry.toUpperCase();
        return sequenceService.getNextCode(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
    }

    @Override
    public String getSuggestedBarcode(String shopId, String industry, String category) {
        String prefix = StringUtils.hasText(category)
                ? String.format("%s_%s", industry.toUpperCase(), category.toUpperCase())
                : industry.toUpperCase();
        String sequence = sequenceService.getNextCode(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_BARCODE);
        String sequenceNumber = sequence.split("_")[2];
        String baseCode = "893" + String.format("%09d", Integer.parseInt(sequenceNumber));
        String checkDigit = calculateEan13CheckDigit(baseCode);
        return baseCode + checkDigit;
    }

    private String calculateEan13CheckDigit(String baseCode) {
        int[] digits = baseCode.chars().map(c -> c - '0').toArray();
        int oddSum = 0;
        int evenSum = 0;
        for (int i = 0; i < 12; i++) {
            if (i % 2 == 0) {
                oddSum += digits[i];
            } else {
                evenSum += digits[i];
            }
        }
        int total = oddSum * 3 + evenSum;
        int checkDigit = (10 - (total % 10)) % 10;
        return String.valueOf(checkDigit);
    }

    public ProductResponse toResponse(BranchProduct branchProduct, Product product) {
        if (branchProduct == null && product == null) {
            return null;
        }
        if (branchProduct == null) {
            return ProductResponse.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .nameTranslations(product.getNameTranslations())
                    .category(product.getCategory())
                    .sku(product.getSku())
                    .costPrice(product.getCostPrice())
                    .defaultPrice(product.getDefaultPrice())
                    .unit(product.getUnit())
                    .description(product.getDescription())
                    .images(product.getImages())
                    .barcode(product.getBarcode())
                    .supplierId(product.getSupplierId())
                    .variants(product.getVariants())
                    .priceHistory(product.getPriceHistory())
                    .active(product.isActive())
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt())
                    .build();
        }
        // Nếu có cả BranchProduct và Product
        return ProductResponse.builder()
                .id(branchProduct.getId())
                .productId(product.getId())
                .name(product.getName())
                .nameTranslations(product.getNameTranslations())
                .category(product.getCategory())
                .sku(product.getSku())
                .costPrice(product.getCostPrice())
                .defaultPrice(product.getDefaultPrice())
                .unit(product.getUnit())
                .description(product.getDescription())
                .images(product.getImages())
                .barcode(product.getBarcode())
                .supplierId(product.getSupplierId())
                .variants(product.getVariants())
                .priceHistory(product.getPriceHistory())
                .active(product.isActive())
                .quantity(branchProduct.getQuantity())
                .minQuantity(branchProduct.getMinQuantity())
                .price(branchProduct.getPrice())
                .branchCostPrice(branchProduct.getBranchCostPrice())
                .discountPrice(branchProduct.getDiscountPrice())
                .discountPercentage(branchProduct.getDiscountPercentage())
                .expiryDate(branchProduct.getExpiryDate())
                .branchVariants(branchProduct.getVariants())
                .branchId(branchProduct.getBranchId())
                .activeInBranch(branchProduct.isActiveInBranch())
                .createdAt(branchProduct.getCreatedAt())
                .updatedAt(branchProduct.getUpdatedAt())
                .build();
    }
}