package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.tableGroup.TableGroupRequest;
import com.example.sales.dto.tableGroup.TableGroupResponse;
import com.example.sales.dto.tableGroup.TableGroupUpdateRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.TableGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/table-groups")
@RequiredArgsConstructor
@Validated
public class TableGroupController {
    private final TableGroupService tableGroupService;

    @PostMapping
    @RequirePermission(Permission.TABLE_UPDATE)
    @Operation(summary = "Tạo nhóm bàn", description = "Ghép nhiều bàn thành 1 nhóm (chỉ phục vụ UI)")
    public ApiResponseDto<TableGroupResponse> create(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @Parameter(description = "ID của chi nhánh") String branchId,
            @Valid @RequestBody TableGroupRequest request) {
        TableGroupResponse created = tableGroupService.create(user.getId(), shopId, branchId, request);
        return ApiResponseDto.success(ApiCode.SUCCESS, created);
    }

    @GetMapping
    @RequirePermission(Permission.TABLE_VIEW)
    @Operation(summary = "Danh sách nhóm bàn", description = "Lấy danh sách nhóm bàn theo chi nhánh")
    public ApiResponseDto<List<TableGroupResponse>> list(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @Parameter(description = "ID của chi nhánh") String branchId) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tableGroupService.list(user.getId(), shopId, branchId));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.TABLE_UPDATE)
    @Operation(summary = "Cập nhật nhóm bàn", description = "Đổi tên hoặc cập nhật danh sách bàn trong nhóm")
    public ApiResponseDto<TableGroupResponse> update(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @PathVariable String id,
            @RequestParam String shopId,
            @RequestParam String branchId,
            @RequestBody TableGroupUpdateRequest request) {
        TableGroupResponse updated = tableGroupService.update(user.getId(), shopId, branchId, id, request);
        return ApiResponseDto.success(ApiCode.SUCCESS, updated);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.TABLE_UPDATE)
    @Operation(summary = "Giải nhóm bàn", description = "Xóa mềm nhóm bàn")
    public ApiResponseDto<?> delete(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @PathVariable String id,
            @RequestParam String shopId,
            @RequestParam String branchId) {
        tableGroupService.delete(user.getId(), shopId, branchId, id);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}

