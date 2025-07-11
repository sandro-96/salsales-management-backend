package com.example.sales.repository;

import com.example.sales.model.ShopUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ShopUserRepository extends MongoRepository<ShopUser, String> {
    Optional<ShopUser> findByShopIdAndUserId(String shopId, String userId);
    List<ShopUser> findByShopId(String shopId);
}
