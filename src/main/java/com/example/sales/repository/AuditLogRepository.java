package com.example.sales.repository;

import com.example.sales.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByTargetIdOrderByCreatedAtDesc(String targetId);
}
