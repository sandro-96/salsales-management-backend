// File: src/main/java/com/example/sales/repository/ShopUserRepository.java
package com.example.sales.repository;

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
    Optional<ShopUser> findByShopIdAndUserIdAndBranchId(String shopId, String userId, String branchId);
    Optional<ShopUser> findByUserIdAndShopIdAndBranchIdAndDeletedFalse(String shopId, String userId, String branchId);
    List<ShopUser> findByUserIdAndShopIdAndDeletedFalse(String userId, String shopId);
    Page<ShopUser> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId, Pageable pageable);
    Page<ShopUser> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);
}
