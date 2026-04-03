package com.example.sales.repository;

import com.example.sales.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByUserIdAndDeletedFalse(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndShopIdAndDeletedFalse(String userId, String shopId, Pageable pageable);

    Page<Notification> findByUserIdAndReadAndDeletedFalse(String userId, boolean read, Pageable pageable);

    Page<Notification> findByUserIdAndShopIdAndReadAndDeletedFalse(String userId, String shopId, boolean read, Pageable pageable);

    long countByUserIdAndReadFalseAndDeletedFalse(String userId);

    long countByUserIdAndShopIdAndReadFalseAndDeletedFalse(String userId, String shopId);

    List<Notification> findByUserIdAndReadFalseAndDeletedFalse(String userId);

    List<Notification> findByUserIdAndShopIdAndReadFalseAndDeletedFalse(String userId, String shopId);

    Optional<Notification> findByIdAndUserIdAndDeletedFalse(String id, String userId);
}
