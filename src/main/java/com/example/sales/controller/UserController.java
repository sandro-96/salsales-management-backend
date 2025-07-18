// File: src/main/java/com/example/sales/controller/UserController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.ChangePasswordRequest;
import com.example.sales.dto.UpdateProfileRequest;
import com.example.sales.model.User;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.UserService;
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
    public ApiResponseDto<User> getCurrentUser(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.USER_INFO, userService.getCurrentUser(user.getId()));
    }

    @PutMapping("/update-profile")
    public ApiResponseDto<User> updateProfile(@AuthenticationPrincipal CustomUserDetails user,
                                              @RequestBody UpdateProfileRequest request) {
        com.example.sales.model.User updated = userService.updateProfile(user.getId(), request.getFullName(), request.getPhone(), request.getBusinessType());
        return ApiResponseDto.success(ApiCode.USER_UPDATED, updated);
    }

    @PostMapping("/change-password")
    public ApiResponseDto<?> changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponseDto.success(ApiCode.PASSWORD_CHANGED);
    }
}
