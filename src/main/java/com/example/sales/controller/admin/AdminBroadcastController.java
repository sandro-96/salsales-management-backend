// File: src/main/java/com/example/sales/controller/admin/AdminBroadcastController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.admin.AdminBroadcastRequest;
import com.example.sales.dto.admin.AdminBroadcastResponse;
import com.example.sales.security.Audited;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.admin.AdminBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/broadcasts")
@RequiredArgsConstructor
@Tag(name = "Admin — Broadcast", description = "Gửi thông báo hệ thống đến nhóm user")
public class AdminBroadcastController {

    private final AdminBroadcastService service;

    @GetMapping
    @Operation(summary = "Danh sách broadcast đã gửi / đang gửi")
    @RequireAdminPermission(AdminPermission.BROADCAST_SEND)
    public ApiResponseDto<Page<AdminBroadcastResponse>> list(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, service.list(pageable));
    }

    @PostMapping
    @Operation(summary = "Gửi broadcast ngay")
    @RequireAdminPermission(AdminPermission.BROADCAST_SEND)
    @Audited(resource = "BROADCAST", action = "SEND", targetLabelExpr = "#req.title")
    public ApiResponseDto<AdminBroadcastResponse> send(
            @Valid @RequestBody AdminBroadcastRequest req,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                service.send(req, admin != null ? admin.getId() : null));
    }
}
