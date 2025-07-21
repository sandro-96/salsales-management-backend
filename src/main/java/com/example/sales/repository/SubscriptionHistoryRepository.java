// File: src/main/java/com/example/sales/repository/SubscriptionHistoryRepository.java
package com.example.sales.repository;

import com.example.sales.model.SubscriptionHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubscriptionHistoryRepository extends MongoRepository<SubscriptionHistory, String> {
    List<SubscriptionHistory> findByShopIdOrderByCreatedAtDesc(String shopId);
}
