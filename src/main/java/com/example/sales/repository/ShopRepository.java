// File: src/main/java/com/example/sales/repository/ShopRepository.java
package com.example.sales.repository;

import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShopRepository extends MongoRepository<Shop, String> {
    Optional<Shop> findByOwnerIdAndDeletedFalse(String ownerId);
    Optional<Shop> findByIdAndDeletedFalse(String id);
    List<Shop> findByPlanExpiryBeforeAndPlanNot(LocalDateTime date, SubscriptionPlan plan);
    List<Shop> findByPlanExpiryBetween(LocalDateTime start, LocalDateTime end);
}
