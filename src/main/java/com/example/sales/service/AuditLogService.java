// File: src/main/java/com/example/sales/service/AuditLogService.java
package com.example.sales.service;

import com.example.sales.model.AuditLog;
import com.example.sales.model.User;
import com.example.sales.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, String shopId, String targetId, String targetType, String action, String description) {
        AuditLog log = AuditLog.builder()
                .userId(user.getId())
                .shopId(shopId)
                .targetId(targetId)
                .targetType(targetType)
                .action(action)
                .description(description)
                .build();

        auditLogRepository.save(log);
    }
}
