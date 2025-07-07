package com.example.sales.service;

import com.example.sales.constant.UserRole;
import com.example.sales.dto.JwtResponse;
import com.example.sales.dto.LoginRequest;
import com.example.sales.dto.RegisterRequest;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import com.example.sales.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;
    private final TokenService tokenService;

    @Value("${app.frontend.verify-url}")
    private String verifyUrl;

    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException(ApiErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBusinessType(request.getBusinessType());

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000));
        user.setVerified(false);

        userRepository.save(user);

        String verifyLink = verifyUrl + "?token=" + token;
        String html = "<p>Xin chào,</p>" +
                "<p>Vui lòng xác thực tài khoản của bạn bằng cách nhấn vào liên kết bên dưới:</p>" +
                "<a href=\"" + verifyLink + "\">Xác thực tài khoản</a>" +
                "<p><i>Liên kết này sẽ hết hạn sau 15 phút.</i></p>";

        mailService.send(user.getEmail(), "Xác thực tài khoản - Sandro Sales", html);
    }


    public JwtResponse login(LoginRequest request) {
        // 1. Xác thực tài khoản và mật khẩu
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        // 2. Lấy user từ DB
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.USER_NOT_FOUND));
        // 3. Kiểm tra đã xác thực chưa
        if (!user.isVerified()) {
            throw new BusinessException(ApiErrorCode.EMAIL_NOT_VERIFIED);
        }
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = tokenService.createRefreshToken(user).getToken();
        return new JwtResponse(accessToken, refreshToken);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.USER_NOT_FOUND));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // 15 phút
        userRepository.save(user);

        System.out.println("Gửi email tới " + email + ": Token đặt lại mật khẩu: " + token);
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new BusinessException(ApiErrorCode.ALREADY_VERIFIED);
        }

        // Tạo token mới và cập nhật expiry
        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setVerificationExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000));

        userRepository.save(user);

        String verifyLink = verifyUrl + "?token=" + newToken;

        String html = "<p>Xin chào,</p>" +
                "<p>Bạn đã yêu cầu gửi lại email xác thực tài khoản.</p>" +
                "<p>Vui lòng xác thực tài khoản của bạn bằng cách nhấn vào liên kết bên dưới:</p>" +
                "<a href=\"" + verifyLink + "\">Xác thực tài khoản</a>" +
                "<p><i>Liên kết này sẽ hết hạn sau 15 phút.</i></p>";

        mailService.send(user.getEmail(), "Gửi lại xác thực tài khoản - Sandro Sales", html);
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.INVALID_TOKEN));

        if (user.isVerified()) {
            throw new BusinessException(ApiErrorCode.ALREADY_VERIFIED);
        }

        if (user.getVerificationExpiry().before(new Date())) {
            throw new BusinessException(ApiErrorCode.TOKEN_EXPIRED);
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiry(null);
        userRepository.save(user);
    }

}
