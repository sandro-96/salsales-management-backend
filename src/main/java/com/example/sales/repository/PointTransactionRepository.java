package com.example.sales.repository;

import com.example.sales.model.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PointTransactionRepository extends MongoRepository<PointTransaction, String> {

    Page<PointTransaction> findByCustomerIdAndShopIdOrderByCreatedAtDesc(
            String customerId, String shopId, Pageable pageable);

    List<PointTransaction> findByReferenceIdAndShopId(String referenceId, String shopId);
}
