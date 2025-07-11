// File: src/main/java/com/example/sales/repository/BranchRepository.java
package com.example.sales.repository;

import com.example.sales.model.Branch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BranchRepository extends MongoRepository<Branch, String> {
    List<Branch> findByShopId(String shopId);
}
