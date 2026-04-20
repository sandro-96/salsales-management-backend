// File: src/main/java/com/example/sales/model/AuditLog.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * Audit trail chung cho mọi hành động quan trọng — bao gồm admin (SHOP/USER/
 * BILLING/BROADCAST/CATALOG/AUTH) và domain (ORDER/PRODUCT...).
 * Chỉ ghi append-only; đọc từ AdminAuditPage hoặc {@code /api/audit/{targetId}}.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("audit_logs")
public class AuditLog extends BaseEntity {

    @Id
    private String id;

    // ==== Legacy fields (shop/domain audit) ====
    @Indexed
    private String userId;
    private String shopId;
    @Indexed
    private String targetId;
    private String targetType;

    @Indexed
    private String action;
    private String description;

    // ==== Admin audit fields ====
    @Indexed
    private String actorId;
    private String actorEmail;

    /** Domain ngắn gọn cho admin: SHOP, USER, BILLING, CATALOG, BROADCAST, AUTH, ... */
    @Indexed
    private String resource;

    private String targetLabel;

    /** Payload tóm tắt (đã mask những field nhạy cảm). */
    private Map<String, Object> metadata;

    private String ip;
    private String userAgent;

    /** SUCCESS / FAIL. */
    private String status;
    private String errorMessage;
}
