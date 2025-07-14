// File: src/main/java/com/example/sales/repository/BranchRepository.java
package com.example.sales.repository;

import com.example.sales.model.Branch;
import com.example.sales.repository.base.SoftDeleteRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends SoftDeleteRepository<Branch, String> {
    List<Branch> findByShopIdAndDeletedFalse(String shopId);
    Optional<Branch> findByIdAndDeletedFalse(String id);

}
