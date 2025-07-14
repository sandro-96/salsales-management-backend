// File: src/main/java/com/example/sales/controller/UserController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.ChangePasswordRequest;
import com.example.sales.dto.UpdateProfileRequest;
import com.example.sales.model.User;
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
    public ApiResponse<User> getCurrentUser(@AuthenticationPrincipal User user) {
        return ApiResponse.success(ApiCode.USER_INFO, userService.getCurrentUser(user));
    }

    @PutMapping("/update-profile")
    public ApiResponse<User> updateProfile(@AuthenticationPrincipal User user,
                                           @RequestBody UpdateProfileRequest request) {
        User updated = userService.updateProfile(user, request.getFullName(), request.getPhone(), request.getBusinessType());
        return ApiResponse.success(ApiCode.USER_UPDATED, updated);
    }

    @PostMapping("/change-password")
    public ApiResponse<?> changePassword(@AuthenticationPrincipal User user,
                                         @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.success(ApiCode.PASSWORD_CHANGED);
    }
}
