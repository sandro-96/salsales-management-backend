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
import com.example.sales.model.BranchProductVariant;
import com.example.sales.model.PriceHistory;
import com.example.sales.model.Product;
import com.example.sales.model.ProductVariant;
import com.example.sales.model.Shop;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.AuditLogService;
import com.example.sales.service.BaseService;
import com.example.sales.service.FileUploadService;
import com.example.sales.service.ProductCatalogService;
import com.example.sales.service.ProductService;
import com.example.sales.service.SequenceService;
import com.example.sales.util.CategoryUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dịch vụ quản lý sản phẩm với mô hình Product (thông tin chung của shop) và BranchProduct (thông tin tại chi nhánh).
 * Hỗ trợ tạo sản phẩm từ shop (với tùy chọn branchIds) hoặc từ branch (tạo Product và BranchProduct cho branch hiện tại).
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends BaseService implements ProductService {
    private static final int MAX_PRICE_HISTORY = 50;

    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;
    private final ProductCache productCache;
    private final SequenceService sequenceService;
    private final ProductMapper productMapper;
    private final ProductSearchHelper productSearchHelper;
    private final FileUploadService fileUploadService;
    private final ProductCatalogService productCatalogService;

    private static final int MAX_PRODUCT_IMAGES = 10;
    /** Số file tối đa mỗi lần gọi upload ảnh biến thể (staging) */
    private static final int MAX_VARIANT_STAGED_FILES = 10;

    @Override
    public ProductResponse createProduct(String shopId, ProductRequest request) {
        // Validate shop
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Lấy tất cả chi nhánh của shop
        List<String> branchIds = branchRepository.findAllByShopIdAndDeletedFalse(shopId)
                .stream().map(Branch::getId).collect(Collectors.toList());

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
        sequenceService.updateNextSequence(shopId, AppConstants.SequencePrefixes.BARCODE_GLOBAL, AppConstants.SequenceTypes.SEQUENCE_TYPE_BARCODE);

        // Create BranchProducts cho tất cả chi nhánh
        List<BranchProduct> branchProducts = createBranchProducts(shop, product, branchIds);

        // Log audit
        logProductCreation(shopId, product, branchIds);

        // Upsert vào internal catalog nếu sản phẩm có barcode
        catalogUpsert(product);

        // Evict toàn bộ cache của shop để danh sách được load lại từ DB
        productCache.evictByShop(shopId);

        // Return response — trả về BranchProduct đầu tiên hoặc product-only nếu không có branch nào
        return productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Override
    public ProductResponse createBranchProduct(String shopId, String branchId, ProductRequest request) {
        // Tạo sản phẩm từ branch: tạo Product cho shop, rồi tạo BranchProduct cho branch hiện tại
        List<String> branchIds = Collections.singletonList(branchId);
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        // Validate barcode uniqueness
        if (StringUtils.hasText(request.getBarcode())) {
            productRepository.findByShopIdAndBarcodeAndDeletedFalse(shopId, request.getBarcode())
                    .ifPresent(p -> { throw new BusinessException(ApiCode.BARCODE_EXISTS); });
        }

        // Generate SKU
        String prefix = generateSkuPrefix(shop, request.getCategory());
        String sku = StringUtils.hasText(request.getSku())
                ? request.getSku()
                : sequenceService.getNextCode(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
        productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku)
                .ifPresent(p -> { throw new BusinessException(ApiCode.SKU_EXISTS); });

        Product product = createNewProduct(shopId, sku, request);
        product = productRepository.save(product);
        sequenceService.updateNextSequence(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
        sequenceService.updateNextSequence(shopId, AppConstants.SequencePrefixes.BARCODE_GLOBAL, AppConstants.SequenceTypes.SEQUENCE_TYPE_BARCODE);

        List<BranchProduct> branchProducts = createBranchProducts(shop, product, branchIds);

        auditLogService.log(null, shopId, product.getId(), "PRODUCT", "CREATED_BY_BRANCH",
                String.format("Tạo sản phẩm '%s' (SKU: %s) từ chi nhánh %s",
                        product.getName(), product.getSku(), branchId));

        // Upsert vào internal catalog nếu sản phẩm có barcode
        catalogUpsert(product);

        productCache.evictByShop(shopId);
        return productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Override
    public ProductResponse updateProduct(String userId, String shopId, String id, ProductRequest request, List<MultipartFile> files) {
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

        // Nếu có file mới → upload lên S3, merge với ảnh giữ lại trong request
        if (files != null && !files.isEmpty()) {
            List<String> newImageUrls = files.stream()
                    .map(f -> fileUploadService.upload(f, "products/" + shopId + "/" + id))
                    .collect(Collectors.toList());
            List<String> merged = new ArrayList<>();
            if (request.getImages() != null) merged.addAll(request.getImages());
            merged.addAll(newImageUrls);
            if (merged.size() > MAX_PRODUCT_IMAGES) {
                throw new BusinessException(ApiCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);
            }
            request.setImages(merged);
        }

        // Save old values for audit
        String oldName = product.getName();
        String oldCategory = product.getCategory();
        String oldBarcode = product.getBarcode();

        // Xóa ảnh bị remove khỏi S3 (ảnh có trong DB nhưng không còn trong request)
        List<String> oldImages = product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();
        List<String> newImages = request.getImages() != null ? request.getImages() : new ArrayList<>();
        oldImages.stream()
                .filter(url -> !newImages.contains(url))
                .forEach(url -> {
                    try { fileUploadService.delete(url); } catch (Exception ignored) {}
                });

        deleteRemovedVariantImages(product, request);

        // Update Product fields only
        updateExistingProduct(product, request, userId);
        product = productRepository.save(product);

        // Log audit
        logProductUpdate(userId, shopId, product, oldName, oldCategory, oldBarcode);

        // Upsert vào internal catalog nếu sản phẩm có barcode
        catalogUpsert(product);

        // Invalidate cache cho toàn shop (vì product info thay đổi ảnh hưởng mọi branch)
        productCache.evictByShop(shopId);

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

        // Auto-track thay đổi giá trước khi ghi đè
        appendBranchPriceHistory(branchProduct, request, userId, product.getDefaultPrice());

        // Cập nhật các field BranchProduct
        branchProduct.setPrice(request.getPrice() != 0 ? request.getPrice() : product.getDefaultPrice());
        branchProduct.setBranchCostPrice(request.getBranchCostPrice());
        branchProduct.setMinQuantity(request.getMinQuantity());
        branchProduct.setDiscountPrice(request.getDiscountPrice());
        branchProduct.setDiscountPercentage(request.getDiscountPercentage());
        branchProduct.setExpiryDate(request.getExpiryDate());
        branchProduct.setVariants(request.getBranchVariants());
        if (product.isTrackInventory()) {
            branchProduct.setQuantity(request.getQuantity());
        }
        branchProduct = branchProductRepository.save(branchProduct);

        // Log audit
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "UPDATED",
                String.format("Cập nhật thông tin chi nhánh cho sản phẩm '%s' (SKU: %s) tại chi nhánh %s. Giá: %s, Tồn kho: %d",
                        product.getName(), product.getSku(), branchId, branchProduct.getPrice(), branchProduct.getQuantity()));

        // Update cache
        ProductResponse response = productMapper.toResponse(branchProduct, product);
        productCache.evictByShop(shopId);

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
        productCache.evictByShop(shopId);
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
        productCache.evictByShop(shopId);
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
        productCache.evictByShop(shopId);

        // Trả về response đại diện (BranchProduct đầu tiên nếu có)
        List<BranchProduct> branchProducts = branchProductRepository.findByProductIdAndDeletedFalse(productId);
        return productMapper.toResponse(branchProducts.isEmpty() ? null : branchProducts.get(0), product);
    }

    @Override
    public ProductResponse toggleTrackInventory(String userId, String shopId, String productId) {
        // Validate shop
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(productId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));
        boolean newTrackInventoryState = !product.isTrackInventory();
        product.setTrackInventory(newTrackInventoryState);
        product = productRepository.save(product);

        // Log audit
        String action = newTrackInventoryState ? "TRACK_INVENTORY_ACTIVATED" : "TRACK_INVENTORY_DEACTIVATED";
        auditLogService.log(userId, shopId, productId, "PRODUCT", action,
                String.format("%s sản phẩm '%s' (SKU: %s) ở cấp shop%s",
                        newTrackInventoryState ? "Kích hoạt" : "Ngưng theo dõi tồn kho",
                        product.getName(), product.getSku(),
                        newTrackInventoryState ? "" : " — đã tắt quantity tại tất cả chi nhánh"));
        // Update cache
        productCache.evictByShop(shopId);
        return productMapper.toResponse(null, product);
    }

    @Override
    public Page<ProductResponse> searchProducts(String shopId, String branchId, ProductSearchRequest request, Pageable pageable) {
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Bước 1: Tìm productIds từ Product theo keyword/category (field chỉ có ở Product)
        // null = không có filter text → không giới hạn productId
        // Set rỗng = có filter nhưng không khớp → trả về trang rỗng ngay
        Set<String> matchingProductIds = productSearchHelper.findMatchingProductIds(shopId, request);

        // Bước 2: Lọc BranchProduct theo productIds + filter riêng của branch (price, activeInBranch)
        List<BranchProduct> branchProducts = productSearchHelper.searchBranchProducts(shopId, branchId, matchingProductIds, request, pageable);
        long total = productSearchHelper.countBranchProducts(shopId, branchId, matchingProductIds, request);

        // Bước 3: Lấy Product info cho các BranchProduct tìm được
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
        Set<String> productIds = branchProductRepository.findByShopIdAndBranchIdAndQuantityLessThanAndDeletedFalse(shopId, branchId, threshold).stream()
                .map(BranchProduct::getProductId)
                .collect(Collectors.toSet());
        return productRepository.findAllById(productIds).stream()
                .map(product -> productMapper.toResponse(null, product))
                .collect(Collectors.toList());
    }

    @Override
    public String getSuggestedSku(String shopId, String industry, String category) {
        String prefix = StringUtils.hasText(category)
                ? String.format("%s_%s", industry.toUpperCase(), CategoryUtils.toSkuSegment(category))
                : industry.toUpperCase();
        return sequenceService.getNextCode(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
    }

    @Override
    public String getSuggestedBarcode(String shopId, String industry, String category) {
        // Dùng prefix GLOBAL duy nhất cho barcode để đảm bảo sequence tăng liên tục
        // bất kể industry/category — tránh trường hợp 2 prefix khác nhau trả về cùng số thứ tự → barcode trùng
        String sequence = sequenceService.getNextCode(shopId,
                AppConstants.SequencePrefixes.BARCODE_GLOBAL,
                AppConstants.SequenceTypes.SEQUENCE_TYPE_BARCODE);
        // sequence format: "BARCODE_001", "BARCODE_002", ...
        String[] parts = sequence.split("_");
        String sequenceNumber = parts[parts.length - 1];
        String baseCode = "893" + String.format("%09d", Integer.parseInt(sequenceNumber));
        String checkDigit = calculateEan13CheckDigit(baseCode);
        return baseCode + checkDigit;
    }

    private void validateBranches(String shopId, List<String> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) return;
        List<Branch> branches = branchRepository.findAllByShopIdAndDeletedFalse(shopId);
        Map<String, Branch> validBranches = branches.stream()
                .collect(Collectors.toMap(Branch::getId, branch -> branch));
        for (String branchId : branchIds) {
            if (StringUtils.hasText(branchId) && !validBranches.containsKey(branchId)) {
                throw new BusinessException(ApiCode.BRANCH_NOT_FOUND);
            }
        }
    }

    private String generateSkuPrefix(Shop shop, String category) {
        return StringUtils.hasText(category)
                ? String.format("%s_%s", shop.getType().getIndustry().name().toUpperCase(), CategoryUtils.toSkuSegment(category))
                : shop.getType().getIndustry().name().toUpperCase();
    }

    private Product createNewProduct(String shopId, String sku, ProductRequest request) {
        return Product.builder()
                .shopId(shopId)
                .name(request.getName())
                .nameTranslations(request.getNameTranslations())
                .category(CategoryUtils.normalize(request.getCategory()))
                .sku(sku)
                .costPrice(request.getCostPrice())
                .defaultPrice(request.getDefaultPrice())
                .unit(request.getUnit())
                .description(request.getDescription())
                .images(request.getImages())
                .barcode(request.getBarcode())
                .supplierId(request.getSupplierId())
                .variants(assignVariantIds(request.getVariants()))
                .priceHistory(new ArrayList<>()) // Bắt đầu rỗng — history sẽ được ghi khi giá thay đổi
                .active(request.isActive())
                .trackInventory(request.isTrackInventory()) // Có theo dõi tồn kho không
                .build();
    }

    private void updateExistingProduct(Product existing, ProductRequest request, String userId) {
        // Auto-track thay đổi giá trước khi ghi đè
        appendProductPriceHistory(existing, request, userId);

        existing.setName(request.getName());
        existing.setNameTranslations(request.getNameTranslations());
        existing.setCategory(CategoryUtils.normalize(request.getCategory()));
        existing.setCostPrice(request.getCostPrice());
        existing.setDefaultPrice(request.getDefaultPrice());
        existing.setUnit(request.getUnit());
        existing.setDescription(request.getDescription());
        existing.setImages(request.getImages());
        existing.setBarcode(request.getBarcode());
        existing.setSupplierId(request.getSupplierId());
        existing.setVariants(assignVariantIds(request.getVariants()));
        existing.setActive(request.isActive());
        existing.setTrackInventory(request.isTrackInventory()); // Có theo dõi tồn kho không
        // priceHistory KHÔNG lấy từ request — được quản lý bởi appendProductPriceHistory()
    }

    /**
     * Tự động sinh variantId (UUID) cho các variant chưa có ID.
     * Giữ nguyên variantId nếu đã tồn tại (trường hợp update).
     */
    private List<ProductVariant> assignVariantIds(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) return variants;
        variants.forEach(v -> {
            if (!StringUtils.hasText(v.getVariantId())) {
                v.setVariantId(UUID.randomUUID().toString());
            }
        });
        return variants;
    }

    private List<BranchProduct> createBranchProducts(Shop shop, Product product, List<String> branchIds) {
        List<BranchProduct> branchProducts = new ArrayList<>();
        if (branchIds == null || branchIds.isEmpty()) return branchProducts;

        // Validate tất cả branchId thuộc shop
        validateBranches(shop.getId(), branchIds);

        // Check for existing BranchProducts
        List<BranchProduct> existingBranchProducts = branchProductRepository
                .findByProductIdAndBranchIdInAndDeletedFalse(product.getId(), branchIds);
        if (!existingBranchProducts.isEmpty()) {
            throw new BusinessException(ApiCode.PRODUCT_EXISTS_IN_BRANCH);
        }

        // Seed BranchProductVariant từ ProductVariant (giá mặc định của từng biến thể)
        List<BranchProductVariant> seededVariants = null;
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            seededVariants = product.getVariants().stream()
                    .map(v -> BranchProductVariant.builder()
                            .variantId(v.getVariantId())
                            .quantity(0)
                            .price(v.getPrice())
                            .branchCostPrice(v.getCostPrice())
                            .build())
                    .collect(Collectors.toList());
        }
        final List<BranchProductVariant> finalSeededVariants = seededVariants;

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
                        .variants(finalSeededVariants)
                        .build();
                branchProducts.add(branchProduct);
            }
        }
        return saveAllBranchProducts(branchProducts);
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

    /**
     * Upsert thông tin sản phẩm vào internal catalog nếu có barcode.
     * Chạy async nên không block luồng chính.
     */
    private void catalogUpsert(Product product) {
        if (!StringUtils.hasText(product.getBarcode())) return;
        productCatalogService.upsert(
                product.getBarcode(),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getImages()
        );
    }

    private List<BranchProduct> saveAllBranchProducts(List<BranchProduct> branchProducts) {
        return branchProductRepository.saveAll(branchProducts);
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
        int total = oddSum + evenSum * 3;
        int checkDigit = (10 - (total % 10)) % 10;
        return String.valueOf(checkDigit);
    }

    @Override
    public List<String> uploadProductImages(String userId, String shopId, String productId, List<MultipartFile> files) {
        // Validate shop
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(productId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        List<String> currentImages = product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();

        // Check image limit
        if (currentImages.size() + files.size() > MAX_PRODUCT_IMAGES) {
            throw new BusinessException(ApiCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);
        }

        // Upload each file to S3 under "products/{shopId}/{productId}/" folder
        String folder = "products/" + shopId + "/" + productId;
        for (MultipartFile file : files) {
            String imageUrl = fileUploadService.upload(file, folder);
            currentImages.add(imageUrl);
        }

        product.setImages(currentImages);
        productRepository.save(product);

        // Invalidate cache for the shop
        productCache.evictByShop(shopId);

        // Log audit
        auditLogService.log(userId, shopId, productId, "PRODUCT", "IMAGES_UPLOADED",
                String.format("Upload %d ảnh cho sản phẩm '%s' (SKU: %s). Tổng ảnh hiện tại: %d",
                        files.size(), product.getName(), product.getSku(), currentImages.size()));

        return currentImages;
    }

    @Override
    public List<String> uploadStagedVariantImages(String userId, String shopId, List<MultipartFile> files) {
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > MAX_VARIANT_STAGED_FILES) {
            throw new BusinessException(ApiCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);
        }

        String folder = "products/" + shopId + "/variant-staged";
        List<String> urls = files.stream()
                .map(f -> fileUploadService.upload(f, folder))
                .toList();

        auditLogService.log(userId, shopId, null, "PRODUCT", "VARIANT_IMAGES_STAGED",
                String.format("Upload %d ảnh staging cho biến thể (shop %s)", urls.size(), shopId));

        return urls;
    }

    @Override
    public void seedBranchProductsForNewBranch(String shopId, String branchId) {
        List<Product> products = productRepository.findAllByShopIdAndDeletedFalse(shopId);
        if (products.isEmpty()) return;

        // Branch vừa được tạo mới → chắc chắn chưa có BranchProduct nào
        // Seed BranchProduct cho tất cả products hiện có của shop
        List<BranchProduct> branchProducts = products.stream()
                .map(p -> BranchProduct.builder()
                        .productId(p.getId())
                        .shopId(shopId)
                        .branchId(branchId)
                        .quantity(0)
                        .minQuantity(0)
                        .price(p.getDefaultPrice())
                        .branchCostPrice(p.getCostPrice())
                        .activeInBranch(p.isActive())
                        .build())
                .collect(Collectors.toList());

        branchProductRepository.saveAll(branchProducts);
        productCache.evictByShop(shopId);
    }

    @Override
    public List<String> deleteProductImage(String userId, String shopId, String productId, String imageUrl) {
        // Validate shop
        shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Find Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(productId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        List<String> currentImages = product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();

        if (!currentImages.contains(imageUrl)) {
            throw new BusinessException(ApiCode.PRODUCT_IMAGE_NOT_FOUND);
        }

        // Delete from S3
        fileUploadService.delete(imageUrl);

        // Remove URL from list
        currentImages.remove(imageUrl);
        product.setImages(currentImages);
        productRepository.save(product);

        // Invalidate cache for the shop
        productCache.evictByShop(shopId);

        // Log audit
        auditLogService.log(userId, shopId, productId, "PRODUCT", "IMAGE_DELETED",
                String.format("Xóa ảnh khỏi sản phẩm '%s' (SKU: %s). Số ảnh còn lại: %d",
                        product.getName(), product.getSku(), currentImages.size()));

        return currentImages;
    }

    private List<String> collectVariantImageUrls(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        return variants.stream()
                .filter(Objects::nonNull)
                .map(ProductVariant::getImages)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private void deleteRemovedVariantImages(Product existing, ProductRequest request) {
        List<String> oldUrls = collectVariantImageUrls(existing.getVariants());
        List<String> newUrls = collectVariantImageUrls(request.getVariants());
        oldUrls.stream()
                .filter(url -> !newUrls.contains(url))
                .forEach(url -> {
                    try {
                        fileUploadService.delete(url);
                    } catch (Exception ignored) {
                    }
                });
    }

    /**
     * Tự động ghi lịch sử giá cho Product (cấp shop).
     * Chỉ append khi defaultPrice hoặc costPrice thực sự thay đổi.
     * Giữ tối đa MAX_PRICE_HISTORY entries (FIFO — xóa entry cũ nhất khi vượt giới hạn).
     */
    private void appendProductPriceHistory(Product product, ProductRequest request, String userId) {
        boolean priceChanged = Math.abs(product.getDefaultPrice() - request.getDefaultPrice()) > 0.001;
        boolean costChanged  = Math.abs(product.getCostPrice()    - request.getCostPrice())    > 0.001;
        if (!priceChanged && !costChanged) return;

        List<PriceHistory> history = new ArrayList<>(
                product.getPriceHistory() != null ? product.getPriceHistory() : List.of()
        );
        history.add(PriceHistory.builder()
                .oldPrice(product.getDefaultPrice())
                .newPrice(request.getDefaultPrice())
                .oldCostPrice(product.getCostPrice())
                .newCostPrice(request.getCostPrice())
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .reason(request.getReason())
                .build());

        if (history.size() > MAX_PRICE_HISTORY) {
            history = history.subList(history.size() - MAX_PRICE_HISTORY, history.size());
        }
        product.setPriceHistory(history);
    }

    /**
     * Tự động ghi lịch sử giá cho BranchProduct (cấp chi nhánh).
     * Chỉ append khi price hoặc branchCostPrice thực sự thay đổi.
     * Giữ tối đa MAX_PRICE_HISTORY entries (FIFO).
     */
    private void appendBranchPriceHistory(BranchProduct branchProduct, BranchProductRequest request,
                                          String userId, double defaultPrice) {
        double newPrice = request.getPrice() != 0 ? request.getPrice() : defaultPrice;
        boolean priceChanged = Math.abs(branchProduct.getPrice()          - newPrice)                   > 0.001;
        boolean costChanged  = Math.abs(branchProduct.getBranchCostPrice() - request.getBranchCostPrice()) > 0.001;
        if (!priceChanged && !costChanged) return;

        List<PriceHistory> history = new ArrayList<>(
                branchProduct.getPriceHistory() != null ? branchProduct.getPriceHistory() : List.of()
        );
        history.add(PriceHistory.builder()
                .oldPrice(branchProduct.getPrice())
                .newPrice(newPrice)
                .oldCostPrice(branchProduct.getBranchCostPrice())
                .newCostPrice(request.getBranchCostPrice())
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .reason(request.getReason())
                .build());

        if (history.size() > MAX_PRICE_HISTORY) {
            history = history.subList(history.size() - MAX_PRICE_HISTORY, history.size());
        }
        branchProduct.setPriceHistory(history);
    }
}



