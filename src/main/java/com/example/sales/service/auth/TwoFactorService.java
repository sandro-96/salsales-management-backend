// File: src/main/java/com/example/sales/service/auth/TwoFactorService.java
package com.example.sales.service.auth;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Quản lý 2FA TOTP: setup (gen secret), verify (bật), disable, verifyCode.
 * Secret lưu ở User.totpSecret (Base32). Bật qua {@link #verify(String, String)}.
 */
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final UserRepository userRepository;

    @Value("${app.twofactor.issuer:SalesManager}")
    private String issuer;

    public SetupResponse setup(String userId) {
        User user = load(userId);
        String secret = TotpCodec.randomBase32Secret();
        user.setTotpSecret(secret);
        user.setTotpEnabled(false);
        userRepository.save(user);
        String uri = TotpCodec.otpAuthUri(issuer, user.getEmail(), secret);
        return new SetupResponse(secret, uri);
    }

    public void verify(String userId, String code) {
        User user = load(userId);
        if (!StringUtils.hasText(user.getTotpSecret())) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (!TotpCodec.verify(user.getTotpSecret(), code)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    public void disable(String userId, String code) {
        User user = load(userId);
        if (!user.isTotpEnabled()) return;
        if (!TotpCodec.verify(user.getTotpSecret(), code)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        userRepository.save(user);
    }

    public boolean verifyCodeForUser(User user, String code) {
        if (!user.isTotpEnabled() || !StringUtils.hasText(user.getTotpSecret())) return false;
        return TotpCodec.verify(user.getTotpSecret(), code);
    }

    private User load(String userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
    }

    public record SetupResponse(String secret, String otpAuthUri) {}
}
