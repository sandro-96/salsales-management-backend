// File: src/main/java/com/example/sales/dto/admin/AdminAuditLogResponse.java
package com.example.sales.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class AdminAuditLogResponse {
    private String id;
    private String actorId;
    private String actorEmail;
    private String resource;
    private String action;
    private String targetId;
    private String targetLabel;
    private Map<String, Object> metadata;
    private String ip;
    private String userAgent;
    private String status;
    private String errorMessage;
    private Instant createdAt;
}
