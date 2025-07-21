// File: src/main/java/com/example/sales/controller/admin/AdminUserController.java
package com.example.sales.controller.admin;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Lấy tất cả người dùng", description = "Chỉ dành cho admin. Trả về danh sách tất cả tài khoản người dùng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công")
    })
    @GetMapping
    public ApiResponseDto<List<User>> getAll() {
        return ApiResponseDto.success(ApiCode.SUCCESS, adminUserService.getAllUsers());
    }

    @Operation(summary = "Lấy người dùng theo ID", description = "Lấy thông tin người dùng theo ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    @GetMapping("/{id}")
    public ApiResponseDto<User> getById(
            @Parameter(description = "ID của người dùng") @PathVariable String id) {
        return ApiResponseDto.success(ApiCode.SUCCESS, adminUserService.getUserById(id));
    }

    @Operation(summary = "Cập nhật thông tin người dùng", description = "Cập nhật thông tin hồ sơ của người dùng bởi admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    @PutMapping("/{id}")
    public ApiResponseDto<User> updateUser(
            @Parameter(description = "ID của người dùng") @PathVariable String id,
            @RequestBody User userUpdate) {
        return ApiResponseDto.success(ApiCode.USER_UPDATED, adminUserService.updateUser(id, userUpdate));
    }

    @Operation(summary = "Xoá người dùng", description = "Admin xoá người dùng khỏi hệ thống. Không thể tự xoá tài khoản của chính mình.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xoá thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể tự xoá chính mình"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteUser(
            @Parameter(description = "ID của người dùng cần xoá") @PathVariable String id,
            @AuthenticationPrincipal @Parameter(hidden = true) User currentUser) {
        if (id.equals(currentUser.getId())) {
            throw new BusinessException(ApiCode.CANNOT_DELETE_SELF);
        }

        adminUserService.deleteUser(id);
        return ApiResponseDto.success(ApiCode.USER_DELETED);
    }
}
