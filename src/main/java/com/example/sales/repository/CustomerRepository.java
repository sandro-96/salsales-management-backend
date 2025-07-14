// File: src/main/java/com/example/sales/repository/CustomerRepository.java
package com.example.sales.repository;

import com.example.sales.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String> {

    List<Customer> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId);

    Optional<Customer> findByIdAndDeletedFalse(String id);
}
