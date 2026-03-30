// File: src/main/java/com/example/sales/service/impl/InventoryServiceImpl.java
package com.example.sales.service.impl;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.InventoryType;
import com.example.sales.dto.inventory.InventoryTransactionResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.InventoryTransaction;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.InventoryTransactionRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.service.AuditLogService;
import com.example.sales.service.InventoryService;
import com.example.sales.cache.ProductCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    @Transactional
    public int importProductQuantity(String userId, String shopId, String branchId, String branchProductId, int quantity, String note) {
        if (quantity <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        BranchProduct branchProduct = findBranchProduct(shopId, branchId, branchProductId);
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(branchProduct.getProductId(), shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));
        if (!product.isTrackInventory()) {
            throw new BusinessException(ApiCode.PRODUCT_NOT_TRACK_INVENTORY);
        }
        int oldQuantity = branchProduct.getQuantity();
        branchProduct.setQuantity(oldQuantity + quantity);
        branchProductRepository.save(branchProduct);

        saveInventoryTransaction(
                shopId, branchId, branchProduct.getId(),
                product, InventoryType.IMPORT, quantity, branchProduct.getQuantity(),
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
    public int exportProductQuantity(String userId, String shopId, String branchId, String branchProductId, int quantity, String note, String referenceId) {
        if (quantity <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        BranchProduct branchProduct = findBranchProduct(shopId, branchId, branchProductId);
        int oldQuantity = branchProduct.getQuantity();

        if (oldQuantity < quantity) {
            log.warn("Không đủ tồn kho để xuất. Số lượng hiện tại: {}, Số lượng yêu cầu: {}", oldQuantity, quantity);
            throw new BusinessException(ApiCode.INSUFFICIENT_STOCK);
        }

        branchProduct.setQuantity(oldQuantity - quantity);
        branchProductRepository.save(branchProduct);

        Product masterProduct = getMasterProduct(branchProduct.getProductId(), shopId);

        saveInventoryTransaction(
                shopId, branchId, branchProduct.getId(),
                masterProduct, InventoryType.EXPORT, quantity, branchProduct.getQuantity(),
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
    public int adjustProductQuantity(String userId, String shopId, String branchId, String branchProductId, int newQuantity, String note) {
        if (newQuantity < 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        BranchProduct branchProduct = findBranchProduct(shopId, branchId, branchProductId);
        int oldQuantity = branchProduct.getQuantity();
        int quantityChange = newQuantity - oldQuantity;

        branchProduct.setQuantity(newQuantity);
        branchProductRepository.save(branchProduct);

        Product masterProduct = getMasterProduct(branchProduct.getProductId(), shopId);

        saveInventoryTransaction(
                shopId, branchId, branchProduct.getId(),
                masterProduct, InventoryType.ADJUSTMENT, quantityChange, branchProduct.getQuantity(),
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
    public Page<InventoryTransactionResponse> getTransactionHistory(String userId, String shopId, String branchId, String branchProductId, Pageable pageable) {
        findBranchProduct(shopId, branchId, branchProductId);

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

        return page.map(t -> mapToInventoryTransactionResponse(t, createdByNameByUserId));
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
            String shopId, String branchId, String branchProductId,
            Product masterProduct, InventoryType type, int quantity, int currentStock,
            String note, String referenceId) {

        InventoryTransaction transaction = InventoryTransaction.builder()
                .shopId(shopId)
                .branchId(branchId)
                .productId(branchProductId)
                .productName(masterProduct.getName())   // [Bug #4 Fix] snapshot
                .sku(masterProduct.getSku())             // [Bug #4 Fix] snapshot
                .type(type)
                .quantity(quantity)
                .currentStock(currentStock)              // [Bug #3 Fix] snapshot tồn kho sau giao dịch
                .note(note)
                .referenceId(referenceId)
                .build();
        inventoryTransactionRepository.save(transaction);
    }

    private InventoryTransactionResponse mapToInventoryTransactionResponse(
            InventoryTransaction transaction, Map<String, String> createdByNameByUserId) {
        String productName = transaction.getProductName() != null ? transaction.getProductName() : "Unknown Product";
        String sku = transaction.getSku() != null ? transaction.getSku() : "";
        String createdBy = transaction.getCreatedBy();
        String createdByName = createdBy != null ? createdByNameByUserId.get(createdBy) : null;

        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .shopId(transaction.getShopId())
                .branchId(transaction.getBranchId())
                .branchProductId(transaction.getProductId())
                .productName(productName)
                .sku(sku)
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .currentStock(transaction.getCurrentStock())
                .note(transaction.getNote())
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();
    }
}