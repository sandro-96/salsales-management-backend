package com.example.sales.repository;

import com.example.sales.model.PayrollRun;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollRunRepository extends MongoRepository<PayrollRun, String> {
    Optional<PayrollRun> findByShopIdAndMonthAndDeletedFalse(String shopId, String month);
}

