// File: src/main/java/com/example/sales/repository/BranchRepository.java
package com.example.sales.repository;

import com.example.sales.model.Branch;
import com.example.sales.repository.base.SoftDeleteRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface BranchRepository extends SoftDeleteRepository<Branch, String> {
    @Query("{ 'shopId': ?0, 'deleted': false }")
    List<Branch> findByShopId(String shopId);
}
