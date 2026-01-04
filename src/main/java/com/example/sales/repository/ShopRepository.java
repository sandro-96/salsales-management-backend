// File: src/main/java/com/example/sales/repository/ShopRepository.java
package com.example.sales.repository;

import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShopRepository extends MongoRepository<Shop, String> {
    Optional<Shop> findByOwnerIdAndDeletedFalse(String ownerId);
    Optional<Shop> findByIdAndDeletedFalse(String id);
    Optional<Shop> findBySlugAndDeletedFalse(String slug);
    List<Shop> findByPlanExpiryBeforeAndPlanNot(LocalDateTime date, SubscriptionPlan plan);
    List<Shop> findByPlanExpiryBetween(LocalDateTime start, LocalDateTime end);
    List<Shop> findByIdInAndDeletedFalse(List<String> ids);
    boolean existsByNameAndDeletedFalse(String name);
    @Query("{ 'deleted': false, $or: [ { 'name': { $regex: ?0, $options: 'i' } }, { 'address': { $regex: ?0, $options: 'i' } } ] }")
    Page<Shop> findByKeyword(String keyword, Pageable pageable);
    boolean existsBySlugAndDeletedFalse(String slug);
}
