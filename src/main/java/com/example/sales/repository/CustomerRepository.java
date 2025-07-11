// File: src/main/java/com/example/sales/repository/CustomerRepository.java
package com.example.sales.repository;

import com.example.sales.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CustomerRepository extends MongoRepository<Customer, String> {
    List<Customer> findByUserId(String userId);
    List<Customer> findByUserIdAndDeletedFalse(String userId);
    List<Customer> findByShopId(String shopId);
    List<Customer> findByShopIdAndBranchId(String shopId, String branchId);
}
