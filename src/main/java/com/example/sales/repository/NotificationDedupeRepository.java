package com.example.sales.repository;

import com.example.sales.model.NotificationDedupe;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationDedupeRepository extends MongoRepository<NotificationDedupe, String> {
    boolean existsByDedupeKey(String dedupeKey);
}
