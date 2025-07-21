// File: src/main/java/com/example/sales/controller/BranchController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePlan;
import com.example.sales.security.RequireRole;
import com.example.sales.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Validated
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Lấy danh sách chi nhánh", description = "Lấy danh sách chi nhánh của cửa hàng với phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách chi nhánh được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<Page<BranchResponse>> getAll(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable) {
        return ApiResponseDto.success(ApiCode.SUCCESS, branchService.getAll(user.getId(), shopId, pageable));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Tạo chi nhánh mới", description = "Tạo một chi nhánh mới cho cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi nhánh được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<BranchResponse> create(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin chi nhánh") BranchRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, branchService.create(user.getId(), shopId, request));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PutMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Cập nhật chi nhánh", description = "Cập nhật thông tin chi nhánh của cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi nhánh được cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Chi nhánh hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<BranchResponse> update(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của chi nhánh") String id,
            @RequestBody @Valid @Parameter(description = "Thông tin cập nhật chi nhánh") BranchRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, branchService.update(user.getId(), shopId, id, request));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @DeleteMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Xóa chi nhánh", description = "Xóa mềm một chi nhánh của cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi nhánh được xóa thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Chi nhánh hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<?> delete(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của chi nhánh") String id) {
        branchService.delete(user.getId(), shopId, id);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
