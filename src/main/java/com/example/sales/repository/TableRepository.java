// File: src/main/java/com/example/sales/repository/TableRepository.java
package com.example.sales.repository;

import com.example.sales.model.Table;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TableRepository extends MongoRepository<Table, String> {
    List<Table> findByShopId(String shopId);
    List<Table> findByShopIdAndBranchId(String shopId, String branchId);
}

