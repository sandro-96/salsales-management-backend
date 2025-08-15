// File: src/main/java/com/example/sales/repository/BranchRepository.java
package com.example.sales.repository;

import com.example.sales.model.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends MongoRepository<Branch, String> {
    Page<Branch> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);
    Optional<Branch> findByIdAndDeletedFalse(String id);
    long countByShopIdAndDeletedFalse(String shopId);
    Optional<Branch> findByIdAndShopIdAndDeletedFalse(String id, String shopId);
}
