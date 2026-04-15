package com.example.sales.repository;

import com.example.sales.model.TableGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TableGroupRepository extends MongoRepository<TableGroup, String> {
    Optional<TableGroup> findByIdAndDeletedFalse(String id);

    List<TableGroup> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId);

    List<TableGroup> findByShopIdAndBranchIdAndDeletedFalseAndTableIdsContains(
            String shopId, String branchId, String tableId);
}

