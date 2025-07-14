// File: src/main/java/com/example/sales/repository/BranchRepository.java
package com.example.sales.repository;

import com.example.sales.model.Branch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends MongoRepository<Branch, String> {
    List<Branch> findByShopIdAndDeletedFalse(String shopId);
    Optional<Branch> findByIdAndDeletedFalse(String id);
}
