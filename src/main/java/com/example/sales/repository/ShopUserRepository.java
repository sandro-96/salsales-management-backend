// File: src/main/java/com/example/sales/repository/ShopUserRepository.java
package com.example.sales.repository;

import com.example.sales.model.ShopUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ShopUserRepository extends MongoRepository<ShopUser, String> {
    @Query("{ 'shopId': ?0, 'userId': ?1, 'deleted': false }")
    Optional<ShopUser> findByShopIdAndUserId(String shopId, String userId);
}
