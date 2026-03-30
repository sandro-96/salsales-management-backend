// File: src/main/java/com/example/sales/repository/PromotionRepository.java
package com.example.sales.repository;

import com.example.sales.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends MongoRepository<Promotion, String> {
    List<Promotion> findByShopIdAndDeletedFalse(String shopId);

    Optional<Promotion> findByIdAndDeletedFalse(String id);

    Page<Promotion> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);

    @Query("{ 'shopId': ?0, '$or': [ { 'branchId': ?1 }, { 'branchId': null } ], 'deleted': false }")
    Page<Promotion> findByShopIdAndBranchScope(String shopId, String branchId, Pageable pageable);
}
