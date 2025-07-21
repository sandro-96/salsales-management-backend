// File: src/main/java/com/example/sales/controller/UserController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.ChangePasswordRequest;
import com.example.sales.dto.UpdateProfileRequest;
import com.example.sales.model.User;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin người dùng hiện tại", description = "Trả về thông tin chi tiết của người dùng đang đăng nhập.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
    })
    public ApiResponseDto<User> getCurrentUser(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.USER_INFO, userService.getCurrentUser(user.getId()));
    }

    @PutMapping("/update-profile")
    @Operation(summary = "Cập nhật thông tin người dùng", description = "Cập nhật họ tên, số điện thoại và loại hình kinh doanh của người dùng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
    })
    public ApiResponseDto<User> updateProfile(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestBody @Valid @Parameter(description = "Thông tin cập nhật hồ sơ") UpdateProfileRequest request) {
        User updated = userService.updateProfile(user.getId(), request.getFullName(), request.getPhone(), request.getBusinessType());
        return ApiResponseDto.success(ApiCode.USER_UPDATED, updated);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu", description = "Cho phép người dùng đổi mật khẩu bằng cách cung cấp mật khẩu hiện tại và mật khẩu mới.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công"),
            @ApiResponse(responseCode = "400", description = "Mật khẩu hiện tại không đúng hoặc dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
    })
    public ApiResponseDto<?> changePassword(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestBody @Valid @Parameter(description = "Thông tin đổi mật khẩu") ChangePasswordRequest request) {
        userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponseDto.success(ApiCode.PASSWORD_CHANGED);
    }
}
