// File: src/main/java/com/example/sales/service/audit/AdminAuditService.java
package com.example.sales.service.audit;

import com.example.sales.model.AuditLog;
import com.example.sales.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Ghi và truy vấn audit log cho các hành động admin. Ghi async, không bao giờ
 * làm hỏng luồng chính.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AuditLogRepository repository;
    private final MongoTemplate mongoTemplate;

    @Async
    public void record(AuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist admin audit log: {}", ex.getMessage());
        }
    }

    public Page<AuditLog> list(String actorId, String resource, String action,
                               String targetId, Instant from, Instant to, Pageable pageable) {
        Criteria c = new Criteria();
        c.and("deleted").ne(true);
        if (StringUtils.hasText(actorId)) c.and("actorId").is(actorId);
        if (StringUtils.hasText(resource)) c.and("resource").is(resource);
        if (StringUtils.hasText(action)) c.and("action").is(action);
        if (StringUtils.hasText(targetId)) c.and("targetId").is(targetId);
        if (from != null || to != null) {
            Criteria createdAt = Criteria.where("createdAt");
            if (from != null) createdAt = createdAt.gte(toLocal(from));
            if (to != null) createdAt = createdAt.lte(toLocal(to));
            c.andOperator(createdAt);
        }

        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        long total = mongoTemplate.count(Query.query(c), AuditLog.class);
        List<AuditLog> rows = mongoTemplate.find(Query.query(c).with(sorted), AuditLog.class);
        return new PageImpl<>(rows, sorted, total);
    }

    /**
     * Shortcut cho producer muốn ghi log không qua aspect.
     */
    public void record(String actorId, String actorEmail, String resource, String action,
                       String targetId, Map<String, Object> metadata,
                       boolean success, String errorMessage) {
        record(AuditLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .resource(resource)
                .action(action)
                .targetId(targetId)
                .metadata(metadata)
                .status(success ? "SUCCESS" : "FAIL")
                .errorMessage(errorMessage)
                .build());
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
