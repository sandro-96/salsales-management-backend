// File: src/main/java/com/example/sales/repository/SubscriptionRepository.java
package com.example.sales.repository;

import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.model.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

    Optional<Subscription> findByShopId(String shopId);

    List<Subscription> findByStatusAndTrialEndsAtBefore(SubscriptionStatus status, LocalDateTime cutoff);

    List<Subscription> findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, LocalDateTime cutoff);

    List<Subscription> findByStatus(SubscriptionStatus status);

    long countByStatus(SubscriptionStatus status);
}
