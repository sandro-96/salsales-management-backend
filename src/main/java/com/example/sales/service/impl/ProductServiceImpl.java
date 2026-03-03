package com.example.sales.service.impl;

import com.example.sales.cache.ProductCache;
import com.example.sales.cache.ProductMapper;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.AppConstants;
import com.example.sales.dto.product.BranchProductRequest;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.helper.ProductSearchHelper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    private final ProductSearchHelper productSearchHelper;

    @Override
    public ProductResponse createProduct(String shopId, List<String> branchIds, ProductRequest request) {
        // Validate shop
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // [Option A] Nếu không truyền branchIds → tự động lấy tất cả chi nhánh của shop
        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = branchRepository.findAllByShopIdAndDeletedFalse(shopId)
                    .stream().map(Branch::getId).collect(Collectors.toList());
        }

        // Validate branches
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
        sequenceService.updateNextSequence(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_BARCODE);

        // Create BranchProducts
        List<BranchProduct> branchProducts = createBranchProducts(shop, product, branchIds, validBranches);

        // Log audit
        logProductCreation(shopId, product, branchIds);

        // Update cache — dùng BranchProduct đầu tiên làm đại diện
        if (!branchProducts.isEmpty()) {
            ProductResponse responseForCache = productMapper.toResponse(branchProducts.get(0), product);
            productCache.update(branchProducts.get(0).getId(), responseForCache);
        }

        // Return response — trả về BranchProduct đầu tiên hoặc product-only nếu không có branch nào
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
    public ProductResponse updateProduct(String userId, String shopId, String id, ProductRequest request) {
        // Validate shop
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Validate barcode uniqueness (if changed)
        if (StringUtils.hasText(request.getBarcode())) {
            productRepository.findByShopIdAndBarcodeAndDeletedFalse(shopId, request.getBarcode())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
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

        // Update Product fields only
        updateExistingProduct(product, request);
        product = productRepository.save(product);

        // Log audit
        logProductUpdate(userId, shopId, product, oldName, oldCategory, oldBarcode);

        // Invalidate cache cho toàn shop (vì product info thay đổi ảnh hưởng mọi branch)
        productCache.remove(shopId, null);

        // Trả về response: lấy BranchProduct đầu tiên làm đại diện (nếu có)
        List<BranchProduct> branchProducts = branchProductRepository.findByProductIdAndDeletedFalse(product.getId());
        return productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Override
    public ProductResponse updateBranchProduct(String userId, String shopId, String branchId, String branchProductId, BranchProductRequest request) {
        // Validate shop và branch
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        // Tìm BranchProduct
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(branchProductId, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Tìm Product gốc
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Lấy shop để kiểm tra trackInventory
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Cập nhật các field BranchProduct
        branchProduct.setPrice(request.getPrice() != 0 ? request.getPrice() : product.getDefaultPrice());
        branchProduct.setBranchCostPrice(request.getBranchCostPrice());
        branchProduct.setMinQuantity(request.getMinQuantity());
        branchProduct.setDiscountPrice(request.getDiscountPrice());
        branchProduct.setDiscountPercentage(request.getDiscountPercentage());
        branchProduct.setExpiryDate(request.getExpiryDate());
        branchProduct.setVariants(request.getBranchVariants());
        if (shop.getType().isTrackInventory()) {
            branchProduct.setQuantity(request.getQuantity());
        }
        branchProduct = branchProductRepository.save(branchProduct);

        // Log audit
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "UPDATED",
                String.format("Cập nhật thông tin chi nhánh cho sản phẩm '%s' (SKU: %s) tại chi nhánh %s. Giá: %s, Tồn kho: %d",
                        product.getName(), product.getSku(), branchId, branchProduct.getPrice(), branchProduct.getQuantity()));

        // Update cache
        ProductResponse response = productMapper.toResponse(branchProduct, product);
        productCache.update(branchProduct.getId(), response);

        return response;
    }


    @Override
    public void deleteProductFromShop(String userId, String shopId, String productId) {
        // Validate shop
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(productId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Soft delete tất cả BranchProduct liên quan
        List<BranchProduct> branchProducts = branchProductRepository.findByProductIdAndDeletedFalse(productId);
        branchProducts.forEach(bp -> bp.setDeleted(true));
        branchProductRepository.saveAll(branchProducts);

        // Soft delete Product
        product.setDeleted(true);
        productRepository.save(product);

        // Log audit
        auditLogService.log(userId, shopId, productId, "PRODUCT", "DELETED",
                String.format("Xóa sản phẩm '%s' (SKU: %s) khỏi toàn bộ shop. Đã xóa %d BranchProduct liên quan.",
                        product.getName(), product.getSku(), branchProducts.size()));

        // Invalidate cache toàn shop
        productCache.remove(shopId, null);
    }

    @Override
    public ProductResponse getProduct(String shopId, String branchId, String id) {
        // Validate shop and branch
        shopRepository.findByIdAndDeletedFalse(shopId)
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
    public ProductResponse toggleActiveInBranch(String userId, String shopId, String branchId, String branchProductId) {
        // Validate shop and branch
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        // Find BranchProduct
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(branchProductId, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Find Product — kiểm tra Product.active, không cho phép bật lại nếu shop đã tắt
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        if (!product.isActive() && !branchProduct.isActiveInBranch()) {
            throw new BusinessException(ApiCode.PRODUCT_SHOP_INACTIVE);
        }

        // Toggle active tại chi nhánh
        branchProduct.setActiveInBranch(!branchProduct.isActiveInBranch());
        branchProduct = branchProductRepository.save(branchProduct);

        // Log audit
        String action = branchProduct.isActiveInBranch() ? "BRANCH_ACTIVATED" : "BRANCH_DEACTIVATED";
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", action,
                String.format("%s sản phẩm '%s' (SKU: %s) tại chi nhánh %s",
                        branchProduct.isActiveInBranch() ? "Kích hoạt bán" : "Ngưng bán",
                        product.getName(), product.getSku(), branchId));

        // Update cache
        ProductResponse response = productMapper.toResponse(branchProduct, product);
        productCache.update(branchProduct.getId(), response);
        return response;
    }

    @Override
    public ProductResponse toggleActiveShop(String userId, String shopId, String productId) {
        // Validate shop
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(productId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        boolean newActiveState = !product.isActive();
        product.setActive(newActiveState);
        product = productRepository.save(product);

        // Khi tắt (false): sync tắt activeInBranch trên tất cả BranchProduct
        // Khi bật (true): KHÔNG tự động bật lại chi nhánh — để từng chi nhánh tự quản lý
        if (!newActiveState) {
            List<BranchProduct> branchProducts = branchProductRepository.findByProductIdAndDeletedFalse(productId);
            branchProducts.forEach(bp -> bp.setActiveInBranch(false));
            branchProductRepository.saveAll(branchProducts);
        }

        // Log audit
        String action = newActiveState ? "SHOP_ACTIVATED" : "SHOP_DEACTIVATED";
        auditLogService.log(userId, shopId, productId, "PRODUCT", action,
                String.format("%s sản phẩm '%s' (SKU: %s) ở cấp shop%s",
                        newActiveState ? "Kích hoạt" : "Ngưng kinh doanh",
                        product.getName(), product.getSku(),
                        newActiveState ? "" : " — đã tắt activeInBranch tại tất cả chi nhánh"));

        // Invalidate cache toàn shop
        productCache.remove(shopId, null);

        // Trả về response đại diện (BranchProduct đầu tiên nếu có)
        List<BranchProduct> branchProducts = branchProductRepository.findByProductIdAndDeletedFalse(productId);
        return productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Override
    public Page<ProductResponse> searchProducts(String shopId, String branchId, ProductSearchRequest request, Pageable pageable) {
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        List<BranchProduct> branchProducts = productSearchHelper.search(shopId, branchId, request, pageable);
        long total = productSearchHelper.count(shopId, branchId, request);

        Set<String> productIds = branchProducts.stream()
                .map(BranchProduct::getProductId)
                .collect(Collectors.toSet());

        Map<String, Product> productsMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ProductResponse> responses = branchProducts.stream()
                .map(bp -> productMapper.toResponse(bp, productsMap.get(bp.getProductId())))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
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

    private void updateExistingProduct(Product existing, ProductRequest request) {
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
    }

    private List<BranchProduct> createBranchProducts(Shop shop, Product product, List<String> branchIds,
                                                     Map<String, Branch> validBranches) {
        List<BranchProduct> branchProducts = new ArrayList<>();
        if (branchIds == null || branchIds.isEmpty()) return branchProducts;

        // Check for existing BranchProducts
        List<BranchProduct> existingBranchProducts = branchProductRepository
                .findByProductIdAndBranchIdInAndDeletedFalse(product.getId(), branchIds);
        if (!existingBranchProducts.isEmpty()) {
            throw new BusinessException(ApiCode.PRODUCT_EXISTS_IN_BRANCH);
        }

        // Khởi tạo BranchProduct với giá mặc định từ Product
        for (String branchId : branchIds) {
            if (StringUtils.hasText(branchId)) {
                BranchProduct branchProduct = BranchProduct.builder()
                        .productId(product.getId())
                        .shopId(shop.getId())
                        .branchId(branchId)
                        .quantity(0) // Tồn kho khởi tạo = 0, cập nhật sau qua updateBranchProduct
                        .minQuantity(0)
                        .price(product.getDefaultPrice()) // Lấy giá mặc định từ Product
                        .branchCostPrice(product.getCostPrice())
                        .activeInBranch(product.isActive())
                        .product(product)
                        .shop(shop)
                        .branch(validBranches.get(branchId))
                        .build();
                branchProducts.add(branchProduct);
            }
        }
        return saveAllBranchProducts(branchProducts).join();
    }

    private void logProductCreation(String shopId, Product product, List<String> branchIds) {
        String branchIdsStr = branchIds == null || branchIds.isEmpty()
                ? "không có chi nhánh"
                : branchIds.stream().filter(StringUtils::hasText).collect(Collectors.joining(", "));
        auditLogService.log(null, shopId, product.getId(), "PRODUCT", "CREATED",
                String.format("Tạo sản phẩm '%s' (SKU: %s) tại các chi nhánh: %s",
                        product.getName(), product.getSku(), branchIdsStr));
    }

    private void logProductUpdate(String userId, String shopId, Product product,
                                  String oldName, String oldCategory, String oldBarcode) {
        auditLogService.log(userId, shopId, product.getId(), "PRODUCT", "UPDATED",
                String.format("Cập nhật thông tin chung sản phẩm '%s' (SKU: %s). " +
                                "Thông tin cũ - Tên: %s, Danh mục: %s, Barcode: %s",
                        product.getName(), product.getSku(), oldName, oldCategory, oldBarcode));
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



