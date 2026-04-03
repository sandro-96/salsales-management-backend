package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.shopUser.AddStaffRequest;
import com.example.sales.dto.shopUser.UpdateStaffRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ShopUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Validated
public class ShopUserManagementController {

    private final ShopUserService shopUserService;

    @GetMapping("/{shopId}/users")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Lấy danh sách thành viên của cửa hàng",
            description = "Trả về danh sách thành viên với tìm kiếm theo tên/email và lọc theo vai trò.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    public ApiResponseDto<?> getUsersInShop(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ShopRole role,
            Pageable pageable
    ) {
        return ApiResponseDto.success(
                ApiCode.SUCCESS,
                shopUserService.getUsersInShop(shopId, customUserDetails.getId(), keyword, role, pageable)
        );
    }

    @PostMapping("/{shopId}/users")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Thêm nhân viên bằng email",
            description = "Tìm user theo email và thêm vào cửa hàng với vai trò được chỉ định.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thêm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng với email này")
    })
    public ApiResponseDto<?> addStaff(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody AddStaffRequest request
    ) {
        return ApiResponseDto.success(
                ApiCode.SUCCESS,
                shopUserService.addUserByEmail(shopId, request.getEmail(), request.getRole(), customUserDetails.getId())
        );
    }

    @PutMapping("/{shopId}/users/{userId}/role")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Cập nhật vai trò nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ApiResponseDto<?> updateRole(
            @PathVariable String shopId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        shopUserService.updateUserRole(shopId, userId, request.getRole(), customUserDetails.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    @PutMapping("/{shopId}/users/{userId}/permissions")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Cập nhật quyền của nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ApiResponseDto<?> updatePermissions(
            @PathVariable String shopId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        shopUserService.updatePermissions(shopId, userId, request.getPermissions(), customUserDetails.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    @DeleteMapping("/{shopId}/users/{userId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Xoá nhân viên khỏi cửa hàng (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xoá thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ApiResponseDto<?> removeStaff(
            @PathVariable String shopId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        shopUserService.removeUser(shopId, userId, customUserDetails.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }
}