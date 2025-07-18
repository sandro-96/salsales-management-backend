// File: src/main/java/com/example/sales/repository/InventoryTransactionRepository.java
package com.example.sales.repository;

import com.example.sales.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryTransactionRepository extends MongoRepository<InventoryTransaction, String> {
    Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
}
