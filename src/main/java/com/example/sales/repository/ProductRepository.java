// File: src/main/java/com/example/sales/repository/ProductRepository.java
package com.example.sales.repository;

import com.example.sales.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    @Query("{ 'shopId': ?0, 'deleted': false }")
    List<Product> findByShopId(String shopId);
}
