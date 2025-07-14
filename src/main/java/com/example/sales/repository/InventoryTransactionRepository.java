// File: src/main/java/com/example/sales/repository/InventoryTransactionRepository.java
package com.example.sales.repository;

import com.example.sales.model.InventoryTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InventoryTransactionRepository extends MongoRepository<InventoryTransaction, String> {
    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(String productId);
}
