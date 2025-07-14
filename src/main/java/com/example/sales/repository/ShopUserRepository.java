// File: src/main/java/com/example/sales/repository/ShopUserRepository.java
package com.example.sales.repository;

import com.example.sales.model.ShopUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ShopUserRepository extends MongoRepository<ShopUser, String> {
    Optional<ShopUser> findByIdAndDeletedFalse(String id);

    Optional<ShopUser> findByShopIdAndUserIdAndDeletedFalse(String shopId, String userId);
}
