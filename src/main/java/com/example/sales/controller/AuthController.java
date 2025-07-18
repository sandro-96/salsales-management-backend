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
    public ApiResponseDto<?> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    @PostMapping("/login")
    public ApiResponseDto<JwtResponse> login(@RequestBody @Valid LoginRequest request) {
        JwtResponse token = authService.login(request);
        return ApiResponseDto.success(ApiCode.SUCCESS, token);
    }

    @PostMapping("/forgot-password")
    public ApiResponseDto<?> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    @PostMapping("/resend-verification")
    public ApiResponseDto<?> resendVerification(@RequestParam String email) {
        authService.resendVerification(email);
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    @GetMapping("/verify")
    public ApiResponseDto<?> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/refresh-token")
    public ApiResponseDto<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String newAccessToken = tokenService.refreshAccessToken(request.getRefreshToken());
        JwtResponse response = new JwtResponse(newAccessToken, request.getRefreshToken());
        return ApiResponseDto.success(ApiCode.SUCCESS, response);
    }

    @PostMapping("/logout")
    public ApiResponseDto<?> logout(@RequestBody RefreshTokenRequest request) {
        tokenService.revokeToken(request.getRefreshToken());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
