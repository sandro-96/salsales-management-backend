package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.*;
import com.example.sales.service.AuthService;
import com.example.sales.service.TokenService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MessageService messageService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody @Valid RegisterRequest request, Locale locale) {
        authService.register(request);
        return ApiResponse.success(ApiMessage.EMAIL_VERIFICATION_SENT, messageService, locale);
    }

    @PostMapping("/login")
    public ApiResponse<JwtResponse> login(@RequestBody @Valid LoginRequest request, Locale locale) {
        JwtResponse token = authService.login(request);
        return ApiResponse.success(ApiMessage.LOGIN_SUCCESS, token, messageService, locale);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestParam String email, Locale locale) {
        authService.forgotPassword(email);
        return ApiResponse.success(ApiMessage.PASSWORD_RESET_SENT, messageService, locale);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<?> resendVerification(@RequestParam String email, Locale locale) {
        authService.resendVerification(email);
        return ApiResponse.success(ApiMessage.VERIFICATION_EMAIL_SENT, messageService, locale);
    }

    @GetMapping("/verify")
    public ApiResponse<?> verify(@RequestParam String token, Locale locale) {
        authService.verifyEmail(token);
        return ApiResponse.success(ApiMessage.VERIFY_SUCCESS, messageService, locale);
    }
    @PostMapping("/refresh-token")
    public ApiResponse<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request, Locale locale) {
        String newAccessToken = tokenService.refreshAccessToken(request.getRefreshToken());
        JwtResponse response = new JwtResponse(newAccessToken, request.getRefreshToken());
        return ApiResponse.success(ApiMessage.REFRESH_SUCCESS, response, messageService, locale);
    }
    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestBody RefreshTokenRequest request, Locale locale) {
        tokenService.revokeToken(request.getRefreshToken());
        return ApiResponse.success(ApiMessage.LOGOUT_SUCCESS, messageService, locale);
    }

}
