// File: src/main/java/com/example/sales/controller/AuthController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.*;
import com.example.sales.service.AuthService;
import com.example.sales.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(ApiCode.EMAIL_SENT);
    }

    @PostMapping("/login")
    public ApiResponse<JwtResponse> login(@RequestBody @Valid LoginRequest request) {
        JwtResponse token = authService.login(request);
        return ApiResponse.success(ApiCode.SUCCESS, token);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ApiResponse.success(ApiCode.EMAIL_SENT);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<?> resendVerification(@RequestParam String email) {
        authService.resendVerification(email);
        return ApiResponse.success(ApiCode.EMAIL_SENT);
    }

    @GetMapping("/verify")
    public ApiResponse<?> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponse.success(ApiCode.SUCCESS);
    }

    @PostMapping("/refresh-token")
    public ApiResponse<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String newAccessToken = tokenService.refreshAccessToken(request.getRefreshToken());
        JwtResponse response = new JwtResponse(newAccessToken, request.getRefreshToken());
        return ApiResponse.success(ApiCode.SUCCESS, response);
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestBody RefreshTokenRequest request) {
        tokenService.revokeToken(request.getRefreshToken());
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
