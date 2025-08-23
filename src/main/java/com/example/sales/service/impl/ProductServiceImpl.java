package com.example.sales.service.impl;

import com.example.sales.cache.ProductCache;
import com.example.sales.cache.ProductMapper;
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
 * Dịch vụ quản lý sản phẩm với mô hình Product (thông tin chung của shop) và BranchProduct (thông tin tại chi nhánh).
 * Hỗ trợ tạo sản phẩm từ shop (với tùy chọn branchIds) hoặc từ branch (tạo Product và BranchProduct cho branch hiện tại).
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
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(String shopId, List<String> branchIds, ProductRequest request) {
        // Validate shop
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Validate branches (if provided)
        Map<String, Branch> validBranches = validateBranches(shopId, branchIds);

        // Validate barcode uniqueness
        if (StringUtils.hasText(request.getBarcode())) {
            productRepository.findByShopIdAndBarcodeAndDeletedFalse(shopId, request.getBarcode())
                    .ifPresent(product -> {
                        throw new BusinessException(ApiCode.BARCODE_EXISTS);
                    });
        }

        // Generate SKU
        String prefix = generateSkuPrefix(shop, request.getCategory());
        String sku = StringUtils.hasText(request.getSku())
                ? request.getSku()
                : sequenceService.getNextCode(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);

        // Check SKU uniqueness
        productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku)
                .ifPresent(product -> {
                    throw new BusinessException(ApiCode.SKU_EXISTS);
                });

        // Create Product
        Product product = createNewProduct(shopId, sku, request);
        product = productRepository.save(product);
        sequenceService.updateNextSequence(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);

        // Create BranchProducts (if branchIds provided)
        List<BranchProduct> branchProducts = createBranchProducts(shop, product, branchIds, validBranches, request);

        // Log audit
        logProductCreation(shopId, product, branchIds);

        // Update cache
        productCache.update(product.getId(), productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product));

        // Return response
        return productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Override
    public ProductResponse createBranchProduct(String shopId, String branchId, ProductRequest request) {
        // Tạo sản phẩm từ branch: tạo Product cho shop, rồi tạo BranchProduct cho branch hiện tại
        List<String> branchIds = Collections.singletonList(branchId);
        ProductResponse response = createProduct(shopId, branchIds, request);

        // Log thêm để track nguồn tạo từ branch
        auditLogService.log(null, shopId, response.getProductId(), "PRODUCT", "CREATED_BY_BRANCH",
                String.format("Tạo sản phẩm '%s' (SKU: %s) từ chi nhánh %s",
                        response.getName(), response.getSku(), branchId));
        return response;
    }

    @Override
    public ProductResponse updateProduct(String userId, String shopId, List<String> branchIds, String id, ProductRequest request) {
        // Validate shop
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Validate branches
        Map<String, Branch> validBranches = validateBranches(shopId, branchIds);

        // Validate barcode uniqueness (if changed)
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

        // Save old values for audit
        String oldName = product.getName();
        String oldCategory = product.getCategory();
        String oldBarcode = product.getBarcode();

        // Update Product
        updateExistingProduct(product, request);
        product = productRepository.save(product);

        // Update or create BranchProducts
        List<BranchProduct> branchProducts = updateOrCreateBranchProducts(shop, product, branchIds, validBranches, request);

        // Log audit
        logProductUpdate(userId, shopId, product, branchIds, oldName, oldCategory, oldBarcode);

        // Update cache
        productCache.update(product.getId(), productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product));

        return productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Override
    public void deleteProduct(String userId, String shopId, String branchId, String id) {
        // Validate shop and branch
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        // Find BranchProduct
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Soft delete BranchProduct
        branchProduct.setDeleted(true);
        branchProductRepository.save(branchProduct);

        // Log audit
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "DELETED",
                String.format("Xóa sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        product.getName(), product.getSku(), branchId, branchProduct.getId()));

        // Update cache
        productCache.remove(branchProduct.getId());
    }

    @Override
    public ProductResponse getProduct(String shopId, String branchId, String id) {
        // Validate shop and branch
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        // Find BranchProduct
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        return productMapper.toResponse(branchProduct, product);
    }

    @Override
    public ProductResponse toggleActive(String userId, String shopId, String branchId, String branchProductId) {
        // Validate shop and branch
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        // Find BranchProduct
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(branchProductId, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Toggle active status
        branchProduct.setActiveInBranch(!branchProduct.isActiveInBranch());
        branchProduct = branchProductRepository.save(branchProduct);

        // Log audit
        String action = branchProduct.isActiveInBranch() ? "ACTIVATED" : "DEACTIVATED";
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", action,
                String.format("%s sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        branchProduct.isActiveInBranch() ? "Kích hoạt bán" : "Ngưng bán",
                        product.getName(), product.getSku(), branchId, branchProduct.getId()));

        // Update cache
        productCache.update(branchProduct.getId(), productMapper.toResponse(branchProduct, product));

        return productMapper.toResponse(branchProduct, product);
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
                .map(bp -> productMapper.toResponse(bp, productsMap.get(bp.getProductId())))
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

    private Map<String, Branch> validateBranches(String shopId, List<String> branchIds) {
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
        return validBranches;
    }

    private String generateSkuPrefix(Shop shop, String category) {
        return StringUtils.hasText(category)
                ? String.format("%s_%s", shop.getType().getIndustry().name().toUpperCase(), category.toUpperCase())
                : shop.getType().getIndustry().name().toUpperCase();
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

    private List<BranchProduct> createBranchProducts(Shop shop, Product product, List<String> branchIds,
                                                     Map<String, Branch> validBranches, ProductRequest request) {
        List<BranchProduct> branchProducts = new ArrayList<>();
        if (branchIds != null && !branchIds.isEmpty()) {
            // Check for existing BranchProducts
            List<BranchProduct> existingBranchProducts = branchProductRepository
                    .findByProductIdAndBranchIdInAndDeletedFalse(product.getId(), branchIds);
            if (!existingBranchProducts.isEmpty()) {
                throw new BusinessException(ApiCode.PRODUCT_EXISTS_IN_BRANCH);
            }

            // Create BranchProduct for each branch
            for (String branchId : branchIds) {
                if (StringUtils.hasText(branchId)) {
                    BranchProduct branchProduct = BranchProduct.builder()
                            .productId(product.getId())
                            .shopId(shop.getId())
                            .branchId(branchId)
                            .quantity(shop.getType().isTrackInventory() ? request.getQuantity() : 0)
                            .minQuantity(request.getMinQuantity())
                            .price(request.getPrice() != 0 ? request.getPrice() : product.getDefaultPrice())
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
            branchProducts = saveAllBranchProducts(branchProducts).join();
        }
        return branchProducts;
    }

    private List<BranchProduct> updateOrCreateBranchProducts(Shop shop, Product product, List<String> branchIds,
                                                             Map<String, Branch> validBranches, ProductRequest request) {
        List<BranchProduct> branchProducts = new ArrayList<>();
        if (branchIds != null && !branchIds.isEmpty()) {
            List<BranchProduct> existingBranchProducts = branchProductRepository
                    .findByProductIdAndBranchIdInAndDeletedFalse(product.getId(), branchIds);
            Map<String, BranchProduct> branchProductMap = existingBranchProducts.stream()
                    .collect(Collectors.toMap(BranchProduct::getBranchId, bp -> bp));

            for (String branchId : branchIds) {
                if (StringUtils.hasText(branchId)) {
                    BranchProduct branchProduct = branchProductMap.getOrDefault(branchId, BranchProduct.builder()
                            .productId(product.getId())
                            .shopId(shop.getId())
                            .branchId(branchId)
                            .product(product)
                            .shop(shop)
                            .branch(validBranches.get(branchId))
                            .build());

                    branchProduct.setQuantity(shop.getType().isTrackInventory() ? request.getQuantity() : 0);
                    branchProduct.setMinQuantity(request.getMinQuantity());
                    branchProduct.setPrice(request.getPrice() != 0 ? request.getPrice() : product.getDefaultPrice());
                    branchProduct.setBranchCostPrice(request.getBranchCostPrice());
                    branchProduct.setDiscountPrice(request.getDiscountPrice());
                    branchProduct.setDiscountPercentage(request.getDiscountPercentage());
                    branchProduct.setExpiryDate(request.getExpiryDate());
                    branchProduct.setVariants(request.getBranchVariants());
                    branchProduct.setActiveInBranch(request.isActive());
                    branchProducts.add(branchProduct);
                }
            }
            branchProducts = saveAllBranchProducts(branchProducts).join();
        }
        return branchProducts;
    }

    private void logProductCreation(String shopId, Product product, List<String> branchIds) {
        String branchIdsStr = branchIds == null || branchIds.isEmpty()
                ? "không có chi nhánh"
                : branchIds.stream().filter(StringUtils::hasText).collect(Collectors.joining(", "));
        auditLogService.log(null, shopId, product.getId(), "PRODUCT", "CREATED",
                String.format("Tạo sản phẩm '%s' (SKU: %s) tại các chi nhánh: %s",
                        product.getName(), product.getSku(), branchIdsStr));
    }

    private void logProductUpdate(String userId, String shopId, Product product, List<String> branchIds,
                                  String oldName, String oldCategory, String oldBarcode) {
        if (branchIds != null && !branchIds.isEmpty()) {
            String branchIdsStr = branchIds.stream().filter(StringUtils::hasText).collect(Collectors.joining(", "));
            auditLogService.log(userId, shopId, product.getId(), "PRODUCT", "UPDATED",
                    String.format("Cập nhật sản phẩm '%s' (SKU: %s) tại các chi nhánh: %s. " +
                                    "Thông tin cũ - Tên: %s, Danh mục: %s, Barcode: %s",
                            product.getName(), product.getSku(), branchIdsStr, oldName, oldCategory, oldBarcode));
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<List<BranchProduct>> saveAllBranchProducts(List<BranchProduct> branchProducts) {
        return CompletableFuture.completedFuture(branchProductRepository.saveAll(branchProducts));
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
}