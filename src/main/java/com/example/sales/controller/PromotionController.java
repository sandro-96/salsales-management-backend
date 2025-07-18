// File: src/main/java/com/example/sales/controller/PromotionController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.promotion.PromotionRequest;
import com.example.sales.dto.promotion.PromotionResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePlan;
import com.example.sales.security.RequireRole;
import com.example.sales.service.PromotionService;
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
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Validated
public class PromotionController {

    private final PromotionService promotionService;

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Lấy danh sách khuyến mãi", description = "Lấy danh sách khuyến mãi của cửa hàng với phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách khuyến mãi được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<Page<PromotionResponse>> getAll(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam(required = false) @Parameter(description = "ID của chi nhánh (tùy chọn)") String branchId,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable) {
        return ApiResponseDto.success(ApiCode.SUCCESS, promotionService.getAll(user.getId(), shopId, branchId, pageable));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PostMapping
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Tạo khuyến mãi mới", description = "Tạo một khuyến mãi mới cho cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Khuyến mãi được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<PromotionResponse> create(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin khuyến mãi") PromotionRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, promotionService.create(user.getId(), shopId, request));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @PutMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Cập nhật khuyến mãi", description = "Cập nhật thông tin khuyến mãi của cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Khuyến mãi được cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Khuyến mãi hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<PromotionResponse> update(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của khuyến mãi") String id,
            @RequestBody @Valid @Parameter(description = "Thông tin cập nhật khuyến mãi") PromotionRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, promotionService.update(user.getId(), shopId, id, request));
    }

    @RequirePlan({SubscriptionPlan.PRO, SubscriptionPlan.ENTERPRISE})
    @DeleteMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Xóa khuyến mãi", description = "Xóa mềm một khuyến mãi của cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Khuyến mãi được xóa thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này hoặc gói dịch vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Khuyến mãi hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<?> delete(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của khuyến mãi") String id) {
        promotionService.delete(user.getId(), shopId, id);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
