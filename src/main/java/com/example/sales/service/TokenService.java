package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.RefreshToken;
import com.example.sales.model.User;
import com.example.sales.repository.RefreshTokenRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(user.getId());
        token.setExpiryDate(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)); // 7 ngày

        return refreshTokenRepository.save(token);
    }

    public String refreshAccessToken(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.REFRESH_TOKEN_INVALID));

        if (token.getExpiryDate().before(new Date())) {
            refreshTokenRepository.delete(token);
            throw new BusinessException(ApiErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.USER_NOT_FOUND));

        return jwtUtil.generateToken(user);
    }

    public void revokeToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }
}
