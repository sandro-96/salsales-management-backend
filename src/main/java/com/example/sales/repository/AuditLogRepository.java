// File: src/main/java/com/example/sales/repository/AuditLogRepository.java
package com.example.sales.repository;

import com.example.sales.model.AuditLog;
import com.example.sales.repository.base.SoftDeleteRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface AuditLogRepository extends SoftDeleteRepository<AuditLog, String> {
    @Query("{ 'targetId': ?0, 'deleted': false }")
    List<AuditLog> findByTargetIdOrderByCreatedAtDesc(String targetId);
}
