// File: src/main/java/com/example/sales/controller/admin/AdminAuditController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.admin.AdminAuditLogResponse;
import com.example.sales.model.AuditLog;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.audit.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin — Audit log", description = "Nhật ký hành động của admin")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    @Operation(summary = "Liệt kê audit log với filter actor/resource/action/target/time")
    @RequireAdminPermission(AdminPermission.AUDIT_VIEW)
    public ApiResponseDto<Page<AdminAuditLogResponse>> list(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLog> data = adminAuditService.list(actorId, resource, action, targetId, from, to, pageable);
        return ApiResponseDto.success(ApiCode.SUCCESS, data.map(this::toResponse));
    }

    private AdminAuditLogResponse toResponse(AuditLog log) {
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorEmail(log.getActorEmail())
                .resource(log.getResource())
                .action(log.getAction())
                .targetId(log.getTargetId())
                .targetLabel(log.getTargetLabel())
                .metadata(log.getMetadata())
                .ip(log.getIp())
                .userAgent(log.getUserAgent())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt() == null ? null : log.getCreatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
