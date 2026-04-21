// File: src/main/java/com/example/sales/repository/ShopUserRepository.java
package com.example.sales.repository;

import com.example.sales.constant.ShopRole;
import com.example.sales.model.ShopUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ShopUserRepository extends MongoRepository<ShopUser, String> {
    Optional<ShopUser> findByIdAndDeletedFalse(String id);
    Optional<ShopUser> findByShopIdAndUserIdAndDeletedFalse(String shopId, String userId);
    Page<ShopUser> findByUserIdAndDeletedFalse(String userId, Pageable pageable);

    List<ShopUser> findAllByUserIdAndDeletedFalse(String userId);
    Optional<ShopUser> findByShopIdAndUserId(String shopId, String userId);
    Page<ShopUser> findByShopId(String shopId, Pageable pageable);
    Page<ShopUser> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);
    Page<ShopUser> findByShopIdAndRoleAndDeletedFalse(String shopId, ShopRole role, Pageable pageable);
}
