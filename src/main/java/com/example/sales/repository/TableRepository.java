// File: src/main/java/com/example/sales/repository/TableRepository.java
package com.example.sales.repository;

import com.example.sales.model.Table;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TableRepository extends MongoRepository<Table, String> {

    List<Table> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId);
    Page<Table> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId, Pageable pageable);

    Optional<Table> findByIdAndDeletedFalse(String id);

    /**
     * Dùng cho QR self-ordering: resolve bàn từ public {@code qrToken}.
     * Token là UUID nên unique toàn hệ thống (xác suất collision ~ 0).
     */
    Optional<Table> findByQrTokenAndDeletedFalse(String qrToken);

    List<Table> findByQrTokenIsNullAndDeletedFalse();
}

