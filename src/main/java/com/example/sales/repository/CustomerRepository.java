// File: src/main/java/com/example/sales/repository/CustomerRepository.java
package com.example.sales.repository;

import com.example.sales.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CustomerRepository extends MongoRepository<Customer, String> {
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Customer> findByUserId(String userId);

    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Customer> findByUserIdAndDeletedFalse(String userId);

    @Query("{ 'shopId': ?0, 'deleted': false }")
    List<Customer> findByShopId(String shopId);

    @Query("{ 'shopId': ?0, 'branchId': ?1, 'deleted': false }")
    List<Customer> findByShopIdAndBranchId(String shopId, String branchId);
}
