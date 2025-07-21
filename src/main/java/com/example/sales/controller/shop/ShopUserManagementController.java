// File: src/main/java/com/example/sales/controller/shop/ShopUserManagementController.java
package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ShopUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops/{shopId}") // Base path cho các API quản lý user trong shop
@RequiredArgsConstructor
@Validated
public class ShopUserManagementController {

    private final ShopUserService shopUserService;

    // ✅ API để thêm người dùng vào chi nhánh
    @PostMapping("/branches/{branchId}/users/{userId}")
    @RequireRole({ShopRole.OWNER, ShopRole.ADMIN, ShopRole.MANAGER})
    @Operation(summary = "Thêm/Tái kích hoạt người dùng vào một chi nhánh",
            description = "Thêm người dùng vào chi nhánh cụ thể với vai trò xác định. " +
                    "Nếu người dùng đã bị xóa, sẽ tái kích hoạt họ.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc trùng lặp"),
            @ApiResponse(responseCode = "403", description = "Không có quyền")
    })
    public ApiResponseDto<?> addUserToBranch(
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID của chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID của người dùng cần thêm") @PathVariable String userId,
            @Parameter(description = "Vai trò của người dùng trong chi nhánh (OWNER, ADMIN, MANAGER, STAFF, CASHIER)")
            @RequestParam ShopRole role, // Sử dụng @RequestParam cho role
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        // Có thể thêm kiểm tra branchId có tồn tại trong shop này không nếu cần thiết
        shopUserService.addUser(shopId, userId, role, branchId, customUserDetails.getId());
        return ApiResponseDto.success(
                ApiCode.SUCCESS,
                String.format("Người dùng %s đã được thêm vào chi nhánh %s của cửa hàng %s với vai trò %s.",
                        userId, branchId, shopId, role)
        );
    }

    // ✅ API mới: Xóa người dùng khỏi một chi nhánh cụ thể
    @DeleteMapping("/branches/{branchId}/users/{userId}")
    @RequireRole({ShopRole.OWNER, ShopRole.ADMIN, ShopRole.MANAGER}) // MANAGER có thể xóa STAFF/CASHIER trong chi nhánh của họ
    @Operation(summary = "Xóa người dùng khỏi một chi nhánh cụ thể",
            description = "Xóa mềm (soft delete) một người dùng khỏi một chi nhánh cụ thể của cửa hàng. " +
                    "Chỉ dành cho các vai trò có quyền quản lý nhân viên.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi xác thực hoặc logic nghiệp vụ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "ShopUser hoặc tài nguyên không tìm thấy")
    })
    public ApiResponseDto<?> removeUserFromBranch(
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID của chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID của người dùng cần xóa khỏi chi nhánh") @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        shopUserService.removeUser(shopId, userId, branchId, customUserDetails.getId());
        return ApiResponseDto.success(
                ApiCode.SUCCESS,
                String.format("Người dùng %s đã được xóa khỏi chi nhánh %s của cửa hàng %s thành công.", userId, branchId, shopId)
        );
    }

    // ✅ API mới: Xóa người dùng khỏi toàn bộ shop (tất cả các chi nhánh)
    @DeleteMapping("/users/{userId}")
    @RequireRole({ShopRole.OWNER, ShopRole.ADMIN}) // Chỉ OWNER hoặc ADMIN mới có thể xóa người dùng khỏi toàn bộ shop
    @Operation(summary = "Xóa người dùng khỏi cửa hàng (tất cả các chi nhánh)",
            description = "Xóa mềm (soft delete) một người dùng khỏi tất cả các chi nhánh của một cửa hàng. " +
                    "Chỉ dành cho các vai trò có quyền quản lý cấp cao.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi xác thực hoặc logic nghiệp vụ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Người dùng không tìm thấy trong cửa hàng")
    })
    public ApiResponseDto<?> removeUserFromShop(
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID của người dùng cần xóa khỏi cửa hàng") @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        shopUserService.removeUserFromShop(shopId, userId, customUserDetails.getId());
        return ApiResponseDto.success(
                ApiCode.SUCCESS,
                String.format("Người dùng %s đã được xóa khỏi cửa hàng %s thành công.", userId, shopId)
        );
    }
}