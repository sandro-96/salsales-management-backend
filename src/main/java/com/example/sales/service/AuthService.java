package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.UserRole;
import com.example.sales.constant.WebSocketMessageType;
import com.example.sales.dto.JwtResponse;
import com.example.sales.dto.LoginRequest;
import com.example.sales.dto.GoogleLoginRequest;
import com.example.sales.dto.RegisterRequest;
import com.example.sales.dto.websocket.WebSocketMessage;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import com.example.sales.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${app.frontend.verify-url}")
    private String verifyUrl;

    @Value("${app.reset-token.expiry-minutes}")
    private long resetTokenExpiryMinutes;

    @Value("${app.google.client-id}")
    private String googleClientId;

    public void register(RegisterRequest request) {
        Optional<User> existingUserOpt = userRepository.findByEmailAndDeletedFalse(request.getEmail());
        String token = UUID.randomUUID().toString();

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.isVerified()) {
                throw new BusinessException(ApiCode.EMAIL_EXISTS);
            }
            // Update unverified user
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            // Create new user
            user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setVerified(false);
            // Set other fields from RegisterRequest as needed
        }
        user.setVerificationToken(token);
        user.setVerificationExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60));
        userRepository.save(user);

        String verifyLink = verifyUrl + "?token=" + token;
        String html = "<p>Xin chào,</p>" +
                "<p>Vui lòng xác thực tài khoản của bạn bằng cách nhấn vào liên kết bên dưới:</p>" +
                "<a href=\"" + verifyLink + "\">Xác thực tài khoản</a>" +
                "<p><i>Liên kết này sẽ hết hạn sau 15 phút.</i></p>";

        mailService.send(user.getEmail(), "Xác thực tài khoản - Sandro Sales", html);
    }

    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ApiCode.INVALID_CREDENTIALS);
        }
        if (!user.isVerified()) {
            throw new BusinessException(ApiCode.EMAIL_NOT_VERIFIED);
        }
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = tokenService.createRefreshToken(user).getToken();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return new JwtResponse(accessToken, refreshToken);
    }

    public JwtResponse loginWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new BusinessException(ApiCode.INVALID_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            Optional<User> existingUserOpt = userRepository.findByGoogleIdAndDeletedFalse(googleId);
            User user;

            if (existingUserOpt.isPresent()) {
                user = existingUserOpt.get();
            } else {
                existingUserOpt = userRepository.findByEmailAndDeletedFalse(email);
                if (existingUserOpt.isPresent()) {
                    user = existingUserOpt.get();
                    user.setGoogleId(googleId);
                } else {
                    user = new User();
                    user.setEmail(email);
                    user.setGoogleId(googleId);
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setVerified(true);
                    user.setRole(UserRole.ROLE_USER);
                }
                user.setPassword(null);
                userRepository.save(user);
            }

            String accessToken = jwtUtil.generateToken(user);
            String refreshToken = tokenService.createRefreshToken(user).getToken();
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            return new JwtResponse(accessToken, refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60));
        userRepository.save(user);

        System.out.println("Gửi email tới " + email + ": Token đặt lại mật khẩu: " + token);
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new BusinessException(ApiCode.ALREADY_VERIFIED);
        }

        // Tạo token mới và cập nhật expiry
        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setVerificationExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60));

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
        Optional<User> optional = userRepository.findByVerificationTokenAndDeletedFalse(token);
        if (optional.isEmpty()) {
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
        User user = optional.get();

        if (user.isVerified()) {
            throw new BusinessException(ApiCode.ALREADY_VERIFIED);
        }

        if (user.getVerificationExpiry() == null || user.getVerificationExpiry().isBefore(Instant.now())) {
            throw new BusinessException(ApiCode.TOKEN_EXPIRED);
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiry(null);
        userRepository.save(user);
        messagingTemplate.convertAndSend(
                "/topic/verify/" + user.getEmail(),
                new WebSocketMessage<>(
                        WebSocketMessageType.EMAIL_VERIFIED,
                        Map.of(
                                "userId", user.getId(),
                                "email", user.getEmail()
                        )
                )
        );
    }
}