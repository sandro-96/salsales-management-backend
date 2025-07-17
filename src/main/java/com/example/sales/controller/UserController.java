// File: src/main/java/com/example/sales/controller/UserController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.ChangePasswordRequest;
import com.example.sales.dto.UpdateProfileRequest;
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
    public ApiResponse<com.example.sales.model.User> getCurrentUser(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success(ApiCode.USER_INFO, userService.getCurrentUser(user.getId()));
    }

    @PutMapping("/update-profile")
    public ApiResponse<com.example.sales.model.User> updateProfile(@AuthenticationPrincipal CustomUserDetails user,
                                           @RequestBody UpdateProfileRequest request) {
        com.example.sales.model.User updated = userService.updateProfile(user.getId(), request.getFullName(), request.getPhone(), request.getBusinessType());
        return ApiResponse.success(ApiCode.USER_UPDATED, updated);
    }

    @PostMapping("/change-password")
    public ApiResponse<?> changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                         @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.success(ApiCode.PASSWORD_CHANGED);
    }
}
