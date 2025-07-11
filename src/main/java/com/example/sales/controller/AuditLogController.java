package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.model.AuditLog;
import com.example.sales.repository.AuditLogRepository;
import com.example.sales.util.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final MessageService messageService;

    @GetMapping("/{targetId}")
    public ApiResponse<List<AuditLog>> getLogs(@PathVariable String targetId, Locale locale) {
        List<AuditLog> logs = auditLogRepository.findByTargetIdOrderByCreatedAtDesc(targetId);
        return ApiResponse.success(ApiMessage.AUDIT_LOG_LIST, logs, messageService, locale);
    }
}
