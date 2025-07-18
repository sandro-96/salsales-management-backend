// File: src/main/java/com/example/sales/controller/AuditLogController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.model.AuditLog;
import com.example.sales.repository.AuditLogRepository;
import com.example.sales.security.RequirePlan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @GetMapping("/{targetId}")
    @Operation(summary = "Lấy nhật ký thay đổi", description = "Trả về danh sách các bản ghi audit log của một thực thể cụ thể, theo thứ tự mới nhất trước.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách nhật ký thành công"),
            @ApiResponse(responseCode = "403", description = "Gói dịch vụ không đủ quyền truy cập")
    })
    public ApiResponseDto<List<AuditLog>> getLogs(
            @PathVariable @Parameter(description = "ID của thực thể cần xem lịch sử thay đổi (ví dụ: đơn hàng, sản phẩm, v.v.)") String targetId) {
        List<AuditLog> logs = auditLogRepository.findByTargetIdAndDeletedFalseOrderByCreatedAtDesc(targetId);
        return ApiResponseDto.success(ApiCode.SUCCESS, logs);
    }
}
