// File: src/main/java/com/example/sales/service/AuditLogService.java
package com.example.sales.service;

import com.example.sales.model.AuditLog;
import com.example.sales.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String userId, String shopId, String targetId, String targetType, String action, String description) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .shopId(shopId)
                .targetId(targetId)
                .targetType(targetType)
                .action(action)
                .description(description)
                .build();

        auditLogRepository.save(log);
    }
}
