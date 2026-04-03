package com.example.sales.repository;

import com.example.sales.model.StaffProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository extends MongoRepository<StaffProfile, String> {
    Optional<StaffProfile> findByShopIdAndUserIdAndDeletedFalse(String shopId, String userId);
    Optional<StaffProfile> findByIdAndShopIdAndDeletedFalse(String id, String shopId);
    List<StaffProfile> findByShopIdAndDeletedFalse(String shopId);
    List<StaffProfile> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId);
    List<StaffProfile> findByShopIdAndUserIdInAndDeletedFalse(String shopId, List<String> userIds);
    List<StaffProfile> findByShopIdAndUserIdIsNullAndDeletedFalse(String shopId);
}
