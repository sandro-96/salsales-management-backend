package com.example.sales.repository;

import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.model.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {

    /**
     * Lookup theo mã tham chiếu gửi sang gateway — unique để IPN callback
     * có thể idempotent khi gateway gọi lại nhiều lần.
     */
    Optional<PaymentTransaction> findByProviderTxnRef(String providerTxnRef);

    Page<PaymentTransaction> findByShopIdOrderByCreatedAtDesc(String shopId, Pageable pageable);

    List<PaymentTransaction> findByShopIdAndStatusOrderByCreatedAtDesc(String shopId,
                                                                      PaymentTransactionStatus status);
}
