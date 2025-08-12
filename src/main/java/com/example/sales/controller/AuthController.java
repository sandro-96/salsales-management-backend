// File: src/main/java/com/example/sales/controller/AuthController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.*;
import com.example.sales.service.AuthService;
import com.example.sales.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Đăng ký tài khoản", description = "Gửi thông tin đăng ký để tạo tài khoản mới và gửi email xác minh.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email xác minh đã được gửi"),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc email đã tồn tại")
    })
    public ApiResponseDto<?> register(
            @RequestBody @Valid @Parameter(description = "Thông tin đăng ký") RegisterRequest request) {
        authService.register(request);
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực người dùng và trả về token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "401", description = "Sai thông tin đăng nhập")
    })
    public ApiResponseDto<JwtResponse> login(
            @RequestBody @Valid @Parameter(description = "Thông tin đăng nhập") LoginRequest request) {
        JwtResponse token = authService.login(request);
        return ApiResponseDto.success(ApiCode.SUCCESS, token);
    }

    @PostMapping("/login/google")
    @Operation(summary = "Đăng nhập bằng Google", description = "Xác thực người dùng qua Google ID token và trả về token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập Google thành công"),
            @ApiResponse(responseCode = "400", description = "ID token không hợp lệ")
    })
    public ApiResponseDto<JwtResponse> loginWithGoogle(
            @RequestBody @Valid @Parameter(description = "Google ID token") GoogleLoginRequest request) {
        JwtResponse token = authService.loginWithGoogle(request);
        return ApiResponseDto.success(ApiCode.SUCCESS, token);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Quên mật khẩu", description = "Gửi email chứa liên kết đặt lại mật khẩu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email đặt lại mật khẩu đã được gửi"),
            @ApiResponse(responseCode = "404", description = "Email không tồn tại")
    })
    public ApiResponseDto<?> forgotPassword(
            @RequestParam @Parameter(description = "Email đã đăng ký") String email) {
        authService.forgotPassword(email);
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Gửi lại email xác minh", description = "Gửi lại email xác minh nếu người dùng chưa xác thực.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email xác minh đã được gửi lại"),
            @ApiResponse(responseCode = "404", description = "Email không tồn tại hoặc đã xác minh")
    })
    public ApiResponseDto<?> resendVerification(
            @RequestParam @Parameter(description = "Email người dùng") String email) {
        authService.resendVerification(email);
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    @GetMapping("/verify")
    @Operation(summary = "Xác minh email", description = "Xác minh tài khoản thông qua token từ email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác minh thành công"),
            @ApiResponse(responseCode = "400", description = "Token không hợp lệ hoặc đã hết hạn")
    })
    public ApiResponseDto<?> verify(
            @RequestParam @Parameter(description = "Mã token xác minh email") String token) {
        authService.verifyEmail(token);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới token", description = "Cung cấp access token mới từ refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Làm mới token thành công"),
            @ApiResponse(responseCode = "401", description = "Refresh token không hợp lệ")
    })
    public ApiResponseDto<JwtResponse> refreshToken(
            @RequestBody @Parameter(description = "Yêu cầu làm mới token") RefreshTokenRequest request) {
        String newAccessToken = tokenService.refreshAccessToken(request.getRefreshToken());
        JwtResponse response = new JwtResponse(newAccessToken, request.getRefreshToken());
        return ApiResponseDto.success(ApiCode.SUCCESS, response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Thu hồi refresh token để kết thúc phiên đăng nhập.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng xuất thành công")
    })
    public ApiResponseDto<?> logout(
            @RequestBody @Parameter(description = "Yêu cầu thu hồi token") RefreshTokenRequest request) {
        tokenService.revokeToken(request.getRefreshToken());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
