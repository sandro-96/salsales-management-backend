// File: src/main/java/com/example/sales/service/impl/InventoryServiceImpl.java
package com.example.sales.service.impl;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.InventoryType;
import com.example.sales.dto.inventory.InventoryTransactionResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.BranchProductVariant;
import com.example.sales.model.InventoryTransaction;
import com.example.sales.model.Order;
import com.example.sales.model.Product;
import com.example.sales.model.ProductVariant;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.InventoryTransactionRepository;
import com.example.sales.repository.OrderRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.service.AuditLogService;
import com.example.sales.service.InventoryService;
import com.example.sales.cache.ProductCache;
import com.example.sales.util.OrderDisplayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ProductCache productCache;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public int importProductQuantity(String userId, String shopId, String branchId, String branchProductId, String variantId, int quantity, String note) {
        if (quantity <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        BranchProduct branchProduct = findBranchProduct(shopId, branchId, branchProductId);
        Product product = getMasterProduct(branchProduct.getProductId(), shopId);
        if (!product.isTrackInventory()) {
            throw new BusinessException(ApiCode.PRODUCT_NOT_TRACK_INVENTORY);
        }

        boolean hasVariants = hasTrackedVariants(branchProduct);
        if (!hasVariants && StringUtils.hasText(variantId)) {
            throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_NOT_ALLOWED);
        }

        if (hasVariants && StringUtils.hasText(variantId)) {
            BranchProductVariant v = requireBranchVariant(branchProduct, variantId);
            int oldVariantQty = v.getQuantity();
            v.setQuantity(oldVariantQty + quantity);
            syncAggregateFromVariants(branchProduct);
            branchProductRepository.save(branchProduct);

            String snapSku = resolveVariantSku(product, variantId);
            saveInventoryTransaction(
                    shopId, branchId, branchProduct.getId(), variantId,
                    product, snapSku, InventoryType.IMPORT, quantity, v.getQuantity(),
                    note, null
            );
            productCache.evictByBranch(shopId, branchId);
            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_IMPORT",
                    String.format("Nhập %d đơn vị biến thể %s của '%s' (SKU: %s) vào chi nhánh %s. Tồn biến thể cũ: %d, mới: %d. Tổng chi nhánh: %d.",
                            quantity, variantId, product.getName(), snapSku, branchId, oldVariantQty, v.getQuantity(), branchProduct.getQuantity()));
            log.info("Nhập thành công {} (variant {}) sản phẩm '{}' cho chi nhánh {}. Tổng tồn: {}",
                    quantity, variantId, product.getName(), branchId, branchProduct.getQuantity());
            return branchProduct.getQuantity();
        }

        if (hasVariants && !StringUtils.hasText(variantId)) {
            int oldQuantity = branchProduct.getQuantity();
            branchProduct.setQuantity(oldQuantity + quantity);
            branchProductRepository.save(branchProduct);
            saveInventoryTransaction(
                    shopId, branchId, branchProduct.getId(), null,
                    product, null, InventoryType.IMPORT, quantity, branchProduct.getQuantity(),
                    note, null
            );
            productCache.evictByBranch(shopId, branchId);
            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_IMPORT",
                    String.format("Nhập %d đơn vị (tổng, không gán biến thể) '%s' vào chi nhánh %s. Tồn cũ: %d, mới: %d.",
                            quantity, product.getName(), branchId, oldQuantity, branchProduct.getQuantity()));
            log.info("Nhập thành công {} sản phẩm '{}' (tổng) cho chi nhánh {}. Số lượng mới: {}",
                    quantity, product.getName(), branchId, branchProduct.getQuantity());
            return branchProduct.getQuantity();
        }

        int oldQuantity = branchProduct.getQuantity();
        branchProduct.setQuantity(oldQuantity + quantity);
        branchProductRepository.save(branchProduct);

        saveInventoryTransaction(
                shopId, branchId, branchProduct.getId(), null,
                product, null, InventoryType.IMPORT, quantity, branchProduct.getQuantity(),
                note, null
        );
        productCache.evictByBranch(shopId, branchId);
        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_IMPORT",
                String.format("Nhập %d đơn vị sản phẩm '%s' (SKU: %s) vào chi nhánh %s. Tồn kho cũ: %d, Tồn kho mới: %d.",
                        quantity, product.getName(), product.getSku(), branchId, oldQuantity, branchProduct.getQuantity()));
        log.info("Nhập thành công {} sản phẩm '{}' cho chi nhánh {}. Số lượng mới: {}",
                quantity, product.getName(), branchId, branchProduct.getQuantity());
        return branchProduct.getQuantity();
    }

    @Override
    @Transactional
    public int exportProductQuantity(String userId, String shopId, String branchId, String branchProductId, String variantId, int quantity, String note, String referenceId) {
        if (quantity <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        BranchProduct branchProduct = findBranchProduct(shopId, branchId, branchProductId);
        Product masterProduct = getMasterProduct(branchProduct.getProductId(), shopId);
        if (!masterProduct.isTrackInventory()) {
            throw new BusinessException(ApiCode.PRODUCT_NOT_TRACK_INVENTORY);
        }

        boolean hasVariants = hasTrackedVariants(branchProduct);
        if (!hasVariants && StringUtils.hasText(variantId)) {
            throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_NOT_ALLOWED);
        }

        if (hasVariants && StringUtils.hasText(variantId)) {
            BranchProductVariant v = requireBranchVariant(branchProduct, variantId);
            int oldVariantQty = v.getQuantity();
            if (oldVariantQty < quantity) {
                log.warn("Không đủ tồn biến thể {} để xuất. Hiện có: {}, yêu cầu: {}", variantId, oldVariantQty, quantity);
                throw new BusinessException(ApiCode.INSUFFICIENT_STOCK);
            }
            v.setQuantity(oldVariantQty - quantity);
            syncAggregateFromVariants(branchProduct);
            branchProductRepository.save(branchProduct);

            String snapSku = resolveVariantSku(masterProduct, variantId);
            saveInventoryTransaction(
                    shopId, branchId, branchProduct.getId(), variantId,
                    masterProduct, snapSku, InventoryType.EXPORT, quantity, v.getQuantity(),
                    note, referenceId
            );

            productCache.evictByBranch(shopId, branchId);

            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_EXPORT",
                    String.format("Xuất %d đơn vị biến thể %s của '%s' (SKU: %s) khỏi chi nhánh %s. Tồn biến thể cũ: %d, mới: %d. Tham chiếu: %s.",
                            quantity, variantId, masterProduct.getName(), snapSku, branchId, oldVariantQty, v.getQuantity(), referenceId));

            log.info("Xuất thành công {} (variant {}) sản phẩm '{}' cho chi nhánh {}. Tổng tồn: {}",
                    quantity, variantId, masterProduct.getName(), branchId, branchProduct.getQuantity());

            return branchProduct.getQuantity();
        }

        if (hasVariants && !StringUtils.hasText(variantId)) {
            int oldQuantity = branchProduct.getQuantity();
            if (oldQuantity < quantity) {
                log.warn("Không đủ tồn kho để xuất. Số lượng hiện tại: {}, Số lượng yêu cầu: {}", oldQuantity, quantity);
                throw new BusinessException(ApiCode.INSUFFICIENT_STOCK);
            }
            branchProduct.setQuantity(oldQuantity - quantity);
            branchProductRepository.save(branchProduct);
            saveInventoryTransaction(
                    shopId, branchId, branchProduct.getId(), null,
                    masterProduct, null, InventoryType.EXPORT, quantity, branchProduct.getQuantity(),
                    note, referenceId
            );
            productCache.evictByBranch(shopId, branchId);
            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_EXPORT",
                    String.format("Xuất %d đơn vị (tổng) '%s' khỏi chi nhánh %s. Tồn cũ: %d, mới: %d. Tham chiếu: %s.",
                            quantity, masterProduct.getName(), branchId, oldQuantity, branchProduct.getQuantity(), referenceId));
            log.info("Xuất thành công {} sản phẩm '{}' (tổng) cho chi nhánh {}. Số lượng mới: {}",
                    quantity, masterProduct.getName(), branchId, branchProduct.getQuantity());
            return branchProduct.getQuantity();
        }

        int oldQuantity = branchProduct.getQuantity();

        if (oldQuantity < quantity) {
            log.warn("Không đủ tồn kho để xuất. Số lượng hiện tại: {}, Số lượng yêu cầu: {}", oldQuantity, quantity);
            throw new BusinessException(ApiCode.INSUFFICIENT_STOCK);
        }

        branchProduct.setQuantity(oldQuantity - quantity);
        branchProductRepository.save(branchProduct);

        saveInventoryTransaction(
                shopId, branchId, branchProduct.getId(), null,
                masterProduct, null, InventoryType.EXPORT, quantity, branchProduct.getQuantity(),
                note, referenceId
        );

        productCache.evictByBranch(shopId, branchId);

        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_EXPORT",
                String.format("Xuất %d đơn vị sản phẩm '%s' (SKU: %s) khỏi chi nhánh %s. Tồn kho cũ: %d, Tồn kho mới: %d. Tham chiếu: %s.",
                        quantity, masterProduct.getName(), masterProduct.getSku(), branchId, oldQuantity, branchProduct.getQuantity(), referenceId));

        log.info("Xuất thành công {} sản phẩm '{}' cho chi nhánh {}. Số lượng mới: {}",
                quantity, masterProduct.getName(), branchId, branchProduct.getQuantity());

        return branchProduct.getQuantity();
    }

    @Override
    @Transactional
    public int adjustProductQuantity(String userId, String shopId, String branchId, String branchProductId, String variantId, int newQuantity, String note) {
        if (newQuantity < 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        BranchProduct branchProduct = findBranchProduct(shopId, branchId, branchProductId);
        Product masterProduct = getMasterProduct(branchProduct.getProductId(), shopId);
        if (!masterProduct.isTrackInventory()) {
            throw new BusinessException(ApiCode.PRODUCT_NOT_TRACK_INVENTORY);
        }

        boolean hasVariants = hasTrackedVariants(branchProduct);
        if (!hasVariants && StringUtils.hasText(variantId)) {
            throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_NOT_ALLOWED);
        }

        if (hasVariants && StringUtils.hasText(variantId)) {
            BranchProductVariant v = requireBranchVariant(branchProduct, variantId);
            int oldVariantQty = v.getQuantity();
            int quantityChange = newQuantity - oldVariantQty;
            v.setQuantity(newQuantity);
            syncAggregateFromVariants(branchProduct);
            branchProductRepository.save(branchProduct);

            String snapSku = resolveVariantSku(masterProduct, variantId);
            saveInventoryTransaction(
                    shopId, branchId, branchProduct.getId(), variantId,
                    masterProduct, snapSku, InventoryType.ADJUSTMENT, quantityChange, v.getQuantity(),
                    note, null
            );

            productCache.evictByBranch(shopId, branchId);

            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_ADJUSTMENT",
                    String.format("Điều chỉnh biến thể %s của '%s' (SKU: %s) tại chi nhánh %s từ %d thành %d. Thay đổi: %s%d.",
                            variantId, masterProduct.getName(), snapSku, branchId, oldVariantQty, newQuantity, quantityChange > 0 ? "+" : "", quantityChange));

            log.info("Điều chỉnh tồn kho biến thể {} sản phẩm '{}' cho chi nhánh {}. Số lượng cũ: {}, Số lượng mới: {}",
                    variantId, masterProduct.getName(), branchId, oldVariantQty, newQuantity);

            return branchProduct.getQuantity();
        }

        if (hasVariants && !StringUtils.hasText(variantId)) {
            int oldQuantity = branchProduct.getQuantity();
            int quantityChange = newQuantity - oldQuantity;
            branchProduct.setQuantity(newQuantity);
            branchProductRepository.save(branchProduct);
            saveInventoryTransaction(
                    shopId, branchId, branchProduct.getId(), null,
                    masterProduct, null, InventoryType.ADJUSTMENT, quantityChange, branchProduct.getQuantity(),
                    note, null
            );
            productCache.evictByBranch(shopId, branchId);
            auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_ADJUSTMENT",
                    String.format("Điều chỉnh tổng tồn '%s' tại chi nhánh %s từ %d thành %d.",
                            masterProduct.getName(), branchId, oldQuantity, newQuantity));
            log.info("Điều chỉnh tổng tồn '{}' cho chi nhánh {}. Số lượng cũ: {}, Số lượng mới: {}",
                    masterProduct.getName(), branchId, oldQuantity, newQuantity);
            return branchProduct.getQuantity();
        }

        int oldQuantity = branchProduct.getQuantity();
        int quantityChange = newQuantity - oldQuantity;

        branchProduct.setQuantity(newQuantity);
        branchProductRepository.save(branchProduct);

        saveInventoryTransaction(
                shopId, branchId, branchProduct.getId(), null,
                masterProduct, null, InventoryType.ADJUSTMENT, quantityChange, branchProduct.getQuantity(),
                note, null
        );

        productCache.evictByBranch(shopId, branchId);

        auditLogService.log(userId, shopId, branchProduct.getId(), "BRANCH_PRODUCT", "INVENTORY_ADJUSTMENT",
                String.format("Điều chỉnh tồn kho sản phẩm '%s' (SKU: %s) tại chi nhánh %s từ %d thành %d. Thay đổi: %s%d.",
                        masterProduct.getName(), masterProduct.getSku(), branchId, oldQuantity, newQuantity, quantityChange > 0 ? "+" : "", quantityChange));

        log.info("Điều chỉnh tồn kho sản phẩm '{}' cho chi nhánh {}. Số lượng cũ: {}, Số lượng mới: {}",
                masterProduct.getName(), branchId, oldQuantity, newQuantity);

        return branchProduct.getQuantity();
    }

    @Override
    @Transactional
    public long exportProductWeightBaseUnits(String userId, String shopId, String branchId,
                                             String branchProductId, long baseUnits,
                                             String note, String referenceId) {
        if (baseUnits <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        BranchProduct bp = findBranchProduct(shopId, branchId, branchProductId);
        Product master = getMasterProduct(bp.getProductId(), shopId);
        if (!master.isTrackInventory()) {
            throw new BusinessException(ApiCode.PRODUCT_NOT_TRACK_INVENTORY);
        }
        if (!master.isSellByWeight()) {
            // Caller đã chọn nhầm path — fail rõ ràng thay vì trộn tồn kho.
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        long current = bp.getStockInBaseUnits() != null ? bp.getStockInBaseUnits() : 0L;
        if (current < baseUnits) {
            log.warn("Không đủ tồn (base units) để xuất SP cân {}: có {}, yêu cầu {}",
                    master.getName(), current, baseUnits);
            throw new BusinessException(ApiCode.INSUFFICIENT_STOCK);
        }
        long next = current - baseUnits;
        bp.setStockInBaseUnits(next);
        branchProductRepository.save(bp);

        saveInventoryTransaction(
                shopId, branchId, bp.getId(), null,
                master, null, InventoryType.EXPORT, toIntQuantity(baseUnits), toIntQuantity(next),
                note, referenceId);
        productCache.evictByBranch(shopId, branchId);
        auditLogService.log(userId, shopId, bp.getId(), "BRANCH_PRODUCT", "INVENTORY_EXPORT_WEIGHT",
                String.format("Xuất %d %s SP cân '%s' (SKU: %s) tại chi nhánh %s. Tồn cũ: %d, mới: %d. Ref: %s.",
                        baseUnits,
                        com.example.sales.util.WeightUnitConverter.baseUnitLabel(master.getUnit()),
                        master.getName(), master.getSku(), branchId, current, next, referenceId));
        return next;
    }

    @Override
    @Transactional
    public long importProductWeightBaseUnits(String userId, String shopId, String branchId,
                                             String branchProductId, long baseUnits,
                                             String note) {
        if (baseUnits <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        BranchProduct bp = findBranchProduct(shopId, branchId, branchProductId);
        Product master = getMasterProduct(bp.getProductId(), shopId);
        if (!master.isTrackInventory()) {
            throw new BusinessException(ApiCode.PRODUCT_NOT_TRACK_INVENTORY);
        }
        if (!master.isSellByWeight()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        long current = bp.getStockInBaseUnits() != null ? bp.getStockInBaseUnits() : 0L;
        long next = current + baseUnits;
        bp.setStockInBaseUnits(next);
        branchProductRepository.save(bp);

        saveInventoryTransaction(
                shopId, branchId, bp.getId(), null,
                master, null, InventoryType.IMPORT, toIntQuantity(baseUnits), toIntQuantity(next),
                note, null);
        productCache.evictByBranch(shopId, branchId);
        auditLogService.log(userId, shopId, bp.getId(), "BRANCH_PRODUCT", "INVENTORY_IMPORT_WEIGHT",
                String.format("Nhập %d %s SP cân '%s' (SKU: %s) tại chi nhánh %s. Tồn cũ: %d, mới: %d.",
                        baseUnits,
                        com.example.sales.util.WeightUnitConverter.baseUnitLabel(master.getUnit()),
                        master.getName(), master.getSku(), branchId, current, next));
        return next;
    }

    @Override
    @Transactional
    public long exportProductWeight(String userId, String shopId, String branchId,
                                    String branchProductId, double weight, String unit,
                                    String note, String referenceId) {
        long baseUnits = resolveBaseUnits(shopId, branchId, branchProductId, weight, unit);
        return exportProductWeightBaseUnits(userId, shopId, branchId, branchProductId,
                baseUnits, note, referenceId);
    }

    @Override
    @Transactional
    public long importProductWeight(String userId, String shopId, String branchId,
                                    String branchProductId, double weight, String unit,
                                    String note) {
        long baseUnits = resolveBaseUnits(shopId, branchId, branchProductId, weight, unit);
        return importProductWeightBaseUnits(userId, shopId, branchId, branchProductId,
                baseUnits, note);
    }

    private long resolveBaseUnits(String shopId, String branchId, String branchProductId,
                                  double weight, String unit) {
        if (weight <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        BranchProduct bp = findBranchProduct(shopId, branchId, branchProductId);
        Product master = getMasterProduct(bp.getProductId(), shopId);
        String effectiveUnit = StringUtils.hasText(unit) ? unit : master.getUnit();
        long baseUnits = com.example.sales.util.WeightUnitConverter
                .toBaseUnits(weight, effectiveUnit);
        if (baseUnits <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        return baseUnits;
    }

    /** InventoryTransaction.quantity/currentStock hiện là int; clamp để tránh overflow. */
    private static int toIntQuantity(long v) {
        if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) v;
    }

    @Override
    public Page<InventoryTransactionResponse> getTransactionHistory(String userId, String shopId, String branchId, String branchProductId, Pageable pageable) {
        BranchProduct bp = findBranchProduct(shopId, branchId, branchProductId);

        auditLogService.log(userId, shopId, branchProductId, "BRANCH_PRODUCT", "INVENTORY_HISTORY_VIEW",
                String.format("Lấy lịch sử giao dịch tồn kho cho sản phẩm '%s' tại chi nhánh %s.", branchProductId, branchId));

        Page<InventoryTransaction> page = inventoryTransactionRepository
                .findByProductIdAndShopIdAndBranchIdOrderByCreatedAtDesc(branchProductId, shopId, branchId, pageable);

        var userIds = page.getContent().stream()
                .map(InventoryTransaction::getCreatedBy)
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, String> createdByNameByUserId = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::displayNameForUser));

        Map<String, Order> orderByReferenceId = new HashMap<>();
        page.getContent().stream()
                .map(InventoryTransaction::getReferenceId)
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(refId -> orderRepository.findByIdAndDeletedFalse(refId)
                        .ifPresent(o -> orderByReferenceId.put(refId, o)));

        Product masterProduct = getMasterProduct(bp.getProductId(), shopId);
        return page.map(t -> mapToInventoryTransactionResponse(t, createdByNameByUserId, masterProduct, orderByReferenceId));
    }

    private String displayNameForUser(User u) {
        if (StringUtils.hasText(u.getFullName())) {
            return u.getFullName();
        }
        return u.getEmail() != null ? u.getEmail() : "";
    }

    private BranchProduct findBranchProduct(String shopId, String branchId, String branchProductId) {
        return branchProductRepository.findByIdAndShopIdAndBranchIdAndDeletedFalse(branchProductId, shopId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));
    }

    private Product getMasterProduct(String productId, String shopId) {
        return productRepository.findByIdAndShopIdAndDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));
    }

    private void saveInventoryTransaction(
            String shopId, String branchId, String branchProductId, String variantId,
            Product masterProduct, String snapshotSku, InventoryType type, int quantity, int currentStock,
            String note, String referenceId) {

        String sku = StringUtils.hasText(snapshotSku) ? snapshotSku : masterProduct.getSku();
        InventoryTransaction transaction = InventoryTransaction.builder()
                .shopId(shopId)
                .branchId(branchId)
                .productId(branchProductId)
                .variantId(variantId)
                .productName(masterProduct.getName())   // [Bug #4 Fix] snapshot
                .sku(sku)             // [Bug #4 Fix] snapshot
                .type(type)
                .quantity(quantity)
                .currentStock(currentStock)              // [Bug #3 Fix] snapshot tồn kho sau giao dịch
                .note(note)
                .referenceId(referenceId)
                .build();
        inventoryTransactionRepository.save(transaction);
    }

    private boolean hasTrackedVariants(BranchProduct bp) {
        return bp.getVariants() != null && !bp.getVariants().isEmpty();
    }

    private void syncAggregateFromVariants(BranchProduct bp) {
        if (!hasTrackedVariants(bp)) {
            return;
        }
        int sum = bp.getVariants().stream().mapToInt(BranchProductVariant::getQuantity).sum();
        bp.setQuantity(sum);
    }

    private BranchProductVariant requireBranchVariant(BranchProduct bp, String variantId) {
        if (!StringUtils.hasText(variantId)) {
            throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_REQUIRED);
        }
        return bp.getVariants().stream()
                .filter(v -> variantId.equals(v.getVariantId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_VARIANT_NOT_FOUND));
    }

    private String resolveVariantSku(Product masterProduct, String variantId) {
        if (!StringUtils.hasText(variantId) || masterProduct.getVariants() == null) {
            return masterProduct.getSku();
        }
        return masterProduct.getVariants().stream()
                .filter(v -> variantId.equals(v.getVariantId()))
                .map(ProductVariant::getSku)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(masterProduct.getSku());
    }

    private String resolveVariantDisplayName(Product masterProduct, String variantId) {
        if (!StringUtils.hasText(variantId) || masterProduct.getVariants() == null) {
            return null;
        }
        return masterProduct.getVariants().stream()
                .filter(v -> variantId.equals(v.getVariantId()))
                .map(ProductVariant::getName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private InventoryTransactionResponse mapToInventoryTransactionResponse(
            InventoryTransaction transaction,
            Map<String, String> createdByNameByUserId,
            Product masterProduct,
            Map<String, Order> orderByReferenceId) {
        String productName = transaction.getProductName() != null ? transaction.getProductName() : "Unknown Product";
        String sku = transaction.getSku() != null ? transaction.getSku() : "";
        String createdBy = transaction.getCreatedBy();
        String createdByName = createdBy != null ? createdByNameByUserId.get(createdBy) : null;
        String variantName = resolveVariantDisplayName(masterProduct, transaction.getVariantId());
        String refId = transaction.getReferenceId();
        Order linkedOrder = refId != null ? orderByReferenceId.get(refId) : null;
        String note = OrderDisplayUtils.enrichInventoryNote(transaction.getNote(), refId, linkedOrder);

        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .shopId(transaction.getShopId())
                .branchId(transaction.getBranchId())
                .branchProductId(transaction.getProductId())
                .variantId(transaction.getVariantId())
                .variantName(variantName)
                .productName(productName)
                .sku(sku)
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .currentStock(transaction.getCurrentStock())
                .note(note)
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();
    }
}