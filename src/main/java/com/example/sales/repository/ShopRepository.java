// File: src/main/java/com/example/sales/repository/ShopRepository.java
package com.example.sales.repository;

import com.example.sales.model.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ShopRepository extends MongoRepository<Shop, String> {
    @Query("{ 'ownerId': ?0, 'deleted': false }")
    Optional<Shop> findByOwnerId(String ownerId);
}
