// File: src/main/java/com/example/sales/controller/TwoFactorController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.Audited;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.auth.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2FA TOTP endpoints cho bất kỳ user nào (không chỉ admin). Khuyến nghị dùng
 * cho admin ở Phase 4; user thường có thể bật để tăng bảo mật.
 */
@RestController
@RequestMapping("/api/me/2fa")
@RequiredArgsConstructor
@Validated
@Tag(name = "Me — 2FA", description = "Bật/tắt 2FA TOTP cho tài khoản hiện tại")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @PostMapping("/setup")
    @Operation(summary = "Sinh secret + otpauth URI cho app Authenticator")
    @Audited(resource = "AUTH", action = "2FA_SETUP")
    public ApiResponseDto<TwoFactorService.SetupResponse> setup(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, twoFactorService.setup(user.getId()));
    }

    @PostMapping("/verify")
    @Operation(summary = "Xác nhận code 6 số để bật 2FA")
    @Audited(resource = "AUTH", action = "2FA_ENABLE")
    public ApiResponseDto<Void> verify(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TotpCodeRequest req
    ) {
        twoFactorService.verify(user.getId(), req.code());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    @PostMapping("/disable")
    @Operation(summary = "Tắt 2FA (cần code hiện tại)")
    @Audited(resource = "AUTH", action = "2FA_DISABLE")
    public ApiResponseDto<Void> disable(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TotpCodeRequest req
    ) {
        twoFactorService.disable(user.getId(), req.code());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    public record TotpCodeRequest(@NotBlank String code) {}
}
