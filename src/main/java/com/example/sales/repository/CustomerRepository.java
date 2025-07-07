package com.example.sales.repository;

import com.example.sales.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CustomerRepository extends MongoRepository<Customer, String> {
    List<Customer> findByUserId(String userId);
    List<Customer> findByUserIdAndDeletedFalse(String userId);
}
