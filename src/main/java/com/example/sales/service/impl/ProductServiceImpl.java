package com.example.sales.service.impl;

import com.example.sales.cache.ProductCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Category;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.CategoryRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.AuditLogService;
import com.example.sales.service.BaseService;
import com.example.sales.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
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
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;
    private final ProductCache productCache;

    @Override
    public ProductResponse createProduct(String shopId, String branchId, ProductRequest request) {
        // Kiểm tra shop tồn tại
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Kiểm tra branch nếu có
        if (StringUtils.hasText(branchId)) {
            branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                    .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));
        }

        // Kiểm tra categoryId nếu có
        if (StringUtils.hasText(request.getCategoryId())) {
            categoryRepository.findByIdAndShopIdAndDeletedFalse(request.getCategoryId(), shopId)
                    .orElseThrow(() -> new BusinessException(ApiCode.CATEGORY_NOT_FOUND));
        }

        // Tạo hoặc cập nhật Product
        String sku = StringUtils.hasText(request.getSku())
                ? request.getSku()
                : UUID.randomUUID().toString();

        Optional<Product> existingProduct = productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku);
        Product product;
        if (existingProduct.isPresent()) {
            product = existingProduct.get();
            // Cập nhật thông tin chung
            product.setName(request.getName());
            product.setNameTranslations(request.getNameTranslations());
            product.setCategoryId(request.getCategoryId());
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
            if (StringUtils.hasText(request.getCategoryId())) {
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new BusinessException(ApiCode.CATEGORY_NOT_FOUND));
                product.setCategory(category);
            } else {
                product.setCategory(null);
            }
            productRepository.save(product);
        } else {
            product = Product.builder()
                    .shopId(shopId)
                    .name(request.getName())
                    .nameTranslations(request.getNameTranslations())
                    .categoryId(request.getCategoryId())
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
            if (StringUtils.hasText(request.getCategoryId())) {
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new BusinessException(ApiCode.CATEGORY_NOT_FOUND));
                product.setCategory(category);
            }
            product = productRepository.save(product);
        }

        // Tạo BranchProduct nếu có branchId
        BranchProduct branchProduct = null;
        if (StringUtils.hasText(branchId)) {
            if (branchProductRepository.findByProductIdAndBranchIdAndDeletedFalse(product.getId(), branchId).isPresent()) {
                throw new BusinessException(ApiCode.PRODUCT_EXISTS_IN_BRANCH);
            }
            branchProduct = BranchProduct.builder()
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
                    .branch(branchRepository.findById(branchId).orElse(null))
                    .build();
            branchProduct = branchProductRepository.save(branchProduct);

            auditLogService.log(null, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "CREATED",
                    String.format("Tạo sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                            product.getName(), product.getSku(), branchProduct.getBranchId(), branchProduct.getId()));
        }

        return toResponse(branchProduct, product);
    }

    @Override
    public ProductResponse updateProduct(String userId, String shopId, String branchId, String id, ProductRequest request) {
        // Kiểm tra shop và branch
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        branchRepository.findByIdAndShopIdAndDeletedFalse(branchId, shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));

        // Kiểm tra categoryId nếu có
        if (StringUtils.hasText(request.getCategoryId())) {
            categoryRepository.findByIdAndShopIdAndDeletedFalse(request.getCategoryId(), shopId)
                    .orElseThrow(() -> new BusinessException(ApiCode.CATEGORY_NOT_FOUND));
        }

        // Tìm BranchProduct
        BranchProduct branchProduct = branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(id, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Tìm Product
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        // Lưu giá trị cũ cho audit log
        String oldName = product.getName();
        String oldCategoryId = product.getCategoryId();
        double oldPrice = branchProduct.getPrice();
        int oldQuantity = branchProduct.getQuantity();
        boolean oldActiveInBranch = branchProduct.isActiveInBranch();
        double oldBranchCostPrice = branchProduct.getBranchCostPrice();
        Double oldDiscountPrice = branchProduct.getDiscountPrice();
        Double oldDiscountPercentage = branchProduct.getDiscountPercentage();

        // Cập nhật Product
        product.setName(request.getName());
        product.setNameTranslations(request.getNameTranslations());
        product.setCategoryId(request.getCategoryId());
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
        if (StringUtils.hasText(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ApiCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        product = productRepository.save(product);

        // Cập nhật BranchProduct
        branchProduct.setPrice(request.getPrice());
        branchProduct.setQuantity(shop.getType().isTrackInventory() ? request.getQuantity() : 0);
        branchProduct.setMinQuantity(request.getMinQuantity());
        branchProduct.setBranchCostPrice(request.getBranchCostPrice());
        branchProduct.setDiscountPrice(request.getDiscountPrice());
        branchProduct.setDiscountPercentage(request.getDiscountPercentage());
        branchProduct.setExpiryDate(request.getExpiryDate());
        branchProduct.setVariants(request.getBranchVariants());
        branchProduct.setActiveInBranch(request.isActive());
        branchProduct.setProduct(product);
        branchProduct.setShop(shop);
        branchProduct.setBranch(branchRepository.findById(branchId).orElse(null));
        branchProduct = branchProductRepository.save(branchProduct);

        // Audit log
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "UPDATED",
                String.format("Cập nhật sản phẩm '%s' (SKU: %s) tại chi nhánh %s. ID BranchProduct: %s",
                        product.getName(), product.getSku(), branchId, branchProduct.getId()));

        return toResponse(branchProduct, product);
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

    public ProductResponse toResponse(BranchProduct branchProduct, Product product) {
        if (branchProduct == null && product == null) {
            return null;
        }
        // Nếu chỉ có Product (trường hợp không có branchId)
        if (branchProduct == null) {
            return ProductResponse.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .nameTranslations(product.getNameTranslations())
                    .categoryId(product.getCategoryId())
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
                .categoryId(product.getCategoryId())
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