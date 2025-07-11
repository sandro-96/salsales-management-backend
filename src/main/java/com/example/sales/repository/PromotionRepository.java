// File: src/main/java/com/example/sales/repository/PromotionRepository.java
package com.example.sales.repository;

import com.example.sales.model.Promotion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PromotionRepository extends MongoRepository<Promotion, String> {
    List<Promotion> findByShopId(String shopId);
    List<Promotion> findByShopIdAndBranchId(String shopId, String branchId);
}
