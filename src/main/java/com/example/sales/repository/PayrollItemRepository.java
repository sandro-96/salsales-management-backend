package com.example.sales.repository;

import com.example.sales.model.PayrollItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollItemRepository extends MongoRepository<PayrollItem, String> {
    List<PayrollItem> findByRunIdAndDeletedFalse(String runId);
    Optional<PayrollItem> findByRunIdAndStaffRefAndDeletedFalse(String runId, String staffRef);
    Optional<PayrollItem> findByShopIdAndMonthAndStaffRefAndDeletedFalse(String shopId, String month, String staffRef);

    /** Prefer newest row if legacy data had duplicates before recomputation fix. */
    Optional<PayrollItem> findFirstByShopIdAndMonthAndStaffRefAndDeletedFalseOrderByCreatedAtDesc(
            String shopId, String month, String staffRef);
}

