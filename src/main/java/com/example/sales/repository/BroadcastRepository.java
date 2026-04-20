// File: src/main/java/com/example/sales/repository/BroadcastRepository.java
package com.example.sales.repository;

import com.example.sales.model.Broadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BroadcastRepository extends MongoRepository<Broadcast, String> {
    Page<Broadcast> findAllByDeletedFalse(Pageable pageable);
}
