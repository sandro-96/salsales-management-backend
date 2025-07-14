// File: src/main/java/com/example/sales/controller/AuditLogController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.model.AuditLog;
import com.example.sales.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Validated
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/{targetId}")
    public ApiResponse<List<AuditLog>> getLogs(@PathVariable String targetId) {
        List<AuditLog> logs = auditLogRepository.findByTargetIdOrderByCreatedAtDesc(targetId);
        return ApiResponse.success(ApiCode.SUCCESS, logs);
    }
}
