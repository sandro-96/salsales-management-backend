// File: src/main/java/com/example/sales/controller/UserController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.ChangePasswordRequest;
import com.example.sales.dto.UpdateProfileRequest;
import com.example.sales.dto.shop.ShopRequest;
import com.example.sales.dto.user.UserResponse;
import com.example.sales.model.User;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.FileUploadService;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final FileUploadService fileUploadService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin người dùng hiện tại", description = "Trả về thông tin chi tiết của người dùng đang đăng nhập.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
    })
    public ApiResponseDto<UserResponse> getCurrentUser(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.USER_INFO, userService.getCurrentUser(user.getId()));
    }

    @PutMapping(path = "/update-profile", consumes = "multipart/form-data")
    @Operation(summary = "Cập nhật thông tin người dùng", description = "Cập nhật thông tin hồ sơ của người dùng bao gồm họ, tên, tên đệm, số điện thoại, địa chỉ, thành phố, bang/tỉnh, mã bưu điện, ảnh đại diện, giới tính và mã quốc gia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
    })
    public ApiResponseDto<UserResponse> updateProfile(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestPart("user") @Valid @Parameter(description = "Thông tin cập nhật hồ sơ") UpdateProfileRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        String avatarUrl = null;
        if (file != null && !file.isEmpty()) {
            avatarUrl = fileUploadService.uploadTemp(file);
            avatarUrl = fileUploadService.move(avatarUrl, "avatar");
            request.setAvatarUrl(avatarUrl);
        }
        UserResponse updated = userService.updateProfile(user.getId(), request);
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
