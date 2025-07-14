// File: com/example/sales/service/InventoryService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.inventory.InventoryRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.InventoryTransaction;
import com.example.sales.model.Product;
import com.example.sales.repository.InventoryTransactionRepository;
import com.example.sales.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryTransaction createTransaction(String shopId, InventoryRequest request) {
        Product product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_NOT_FOUND));

        int change = switch (request.getType()) {
            case IMPORT, ADJUSTMENT -> request.getQuantity();
            case EXPORT -> -request.getQuantity();
        };

        int newQty = product.getQuantity() + change;
        if (newQty < 0) {
            throw new BusinessException(ApiCode.PRODUCT_OUT_OF_STOCK);
        }

        product.setQuantity(newQty);
        productRepository.save(product);

        InventoryTransaction tx = InventoryTransaction.builder()
                .shopId(shopId)
                .branchId(request.getBranchId())
                .productId(product.getId())
                .type(request.getType())
                .quantity(change)
                .note(request.getNote())
                .build();

        return inventoryTransactionRepository.save(tx);
    }

    public List<InventoryTransaction> getHistory(String productId) {
        return inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}
