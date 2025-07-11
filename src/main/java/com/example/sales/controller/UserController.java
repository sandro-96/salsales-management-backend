// File: src/main/java/com/example/sales/controller/UserController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.ChangePasswordRequest;
import com.example.sales.dto.UpdateProfileRequest;
import com.example.sales.model.User;
import com.example.sales.service.UserService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final MessageService messageService;

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(@AuthenticationPrincipal User user, Locale locale) {
        return ApiResponse.success(ApiMessage.USER_INFO, userService.getCurrentUser(user), messageService, locale);
    }

    @PutMapping("/update-profile")
    public ApiResponse<User> updateProfile(@AuthenticationPrincipal User user,
                                           @RequestBody UpdateProfileRequest request,
                                           Locale locale) {
        User updated = userService.updateProfile(user, request.getFullName(), request.getPhone(), request.getBusinessType());
        return ApiResponse.success(ApiMessage.USER_UPDATED, updated, messageService, locale);
    }

    @PostMapping("/change-password")
    public ApiResponse<?> changePassword(@AuthenticationPrincipal User user,
                                         @RequestBody @Valid ChangePasswordRequest request,
                                         Locale locale) {
        userService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.success(ApiMessage.PASSWORD_CHANGED, messageService, locale);
    }
}
