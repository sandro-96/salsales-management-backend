// File: src/main/java/com/example/sales/repository/TableRepository.java
package com.example.sales.repository;

import com.example.sales.model.Table;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TableRepository extends MongoRepository<Table, String> {
    @Query("{ 'shopId': ?0, 'branchId': ?1, 'deleted': false }")
    List<Table> findByShopIdAndBranchId(String shopId, String branchId);

    @Query("{ 'shopId': ?0, 'deleted': false }")
    List<Table> findByShopId(String shopId);

    @Query("{ 'shopId': ?0, 'name': ?1, 'deleted': false }")
    Optional<Table> findByShopIdAndName(String shopId, String name);
}

