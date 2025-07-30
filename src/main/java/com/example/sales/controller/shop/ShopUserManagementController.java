// File: src/main/java/com/example/sales/controller/shop/ShopUserManagementController.java
package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ShopUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops") // Base path cho các API quản lý user trong shop
@RequiredArgsConstructor
@Validated
public class ShopUserManagementController {

    private final ShopUserService shopUserService;

    @GetMapping("/{shopId}/users")
    @RequirePermission(Permission.SHOP_USER_VIEW)
    @Operation(summary = "Lấy danh sách người dùng của cửa hàng",
            description = "Trả về danh sách tất cả người dùng trong một cửa hàng cụ thể với phân trang. " +
                    "Có thể lọc theo chi nhánh bằng tham số branchId. Chỉ dành cho các vai trò có quyền quản lý (OWNER hoặc ADMIN).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng hoặc chi nhánh")
    })
    public ApiResponseDto<?> getUsersInShop(
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID của chi nhánh (tùy chọn, để lọc người dùng theo chi nhánh)") @RequestParam(required = false) String branchId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") @RequestParam(required = false) Pageable pageable
    ) {
        return ApiResponseDto.success(
                ApiCode.SUCCESS,
                shopUserService.getUsersInShop(shopId, customUserDetails.getId(), branchId, pageable)
        );
    }
}