// File: src/main/java/com/example/sales/repository/PromotionRepository.java
package com.example.sales.repository;

import com.example.sales.model.Promotion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface PromotionRepository extends MongoRepository<Promotion, String> {
    @Query("{ 'shopId': ?0, 'deleted': false }")
    List<Promotion> findByShopId(String shopId);

    @Query("{ 'shopId': ?0, 'branchId': ?1, 'deleted': false }")
    List<Promotion> findByShopIdAndBranchId(String shopId, String branchId);
}
