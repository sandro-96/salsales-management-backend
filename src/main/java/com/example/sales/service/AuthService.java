package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Country;
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
import com.example.sales.util.FileUtil;
import com.example.sales.util.PhoneUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    private final FileUploadService fileUploadService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${app.frontend.verify-url}")
    private String verifyUrl;

    @Value("${app.fe.url:}")
    private String feUrl;

    @Value("${app.brand.name:Sổ thu chi}")
    private String brandName;

    @Value("${app.reset-token.expiry-minutes}")
    private long resetTokenExpiryMinutes;

    @Value("${app.google.client-id}")
    private String googleClientId;

    public void register(RegisterRequest request) {
        String countryCode = StringUtils.hasText(request.getCountryCode())
                ? request.getCountryCode().trim()
                : "VN";
        Country country = Country.fromCode(countryCode);
        String phoneCompact = PhoneUtils.compact(request.getPhone());
        if (!StringUtils.hasText(phoneCompact) || !phoneCompact.matches(country.getPhonePattern())) {
            throw new BusinessException(ApiCode.INVALID_PHONE_NUMBER);
        }
        String phoneNormalized = PhoneUtils.normalizeForMatch(phoneCompact);

        Optional<User> existingUserOpt = userRepository.findByEmailAndDeletedFalse(request.getEmail());
        String token = UUID.randomUUID().toString();

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.isVerified()) {
                throw new BusinessException(ApiCode.EMAIL_EXISTS);
            }
        } else {
            // Create new user
            user = new User();
            user.setEmail(request.getEmail());
            user.setVerified(false);
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setMiddleName(request.getMiddleName());
        }
        user.setPhone(phoneCompact);
        user.setPhoneNormalized(phoneNormalized);
        user.setCountryCode(countryCode);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerificationToken(token);
        user.setVerificationExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60));
        userRepository.save(user);

        String verifyLink = verifyUrl + "?token=" + token;
        String html = "<p>Xin chào,</p>" +
                "<p>Vui lòng xác thực tài khoản của bạn bằng cách nhấn vào liên kết bên dưới:</p>" +
                "<a href=\"" + verifyLink + "\">Xác thực tài khoản</a>" +
                "<p><i>Liên kết này sẽ hết hạn sau 15 phút.</i></p>";

        mailService.send(user.getEmail(), "Xác thực tài khoản - " + brandName, html);
    }

    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));

        if (!StringUtils.hasText(user.getPassword())
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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
            String avatarUrl = (String) payload.get("picture");

            Optional<User> existingUserOpt = userRepository.findByGoogleIdAndDeletedFalse(googleId);
            User user;

            if (existingUserOpt.isPresent()) {
                user = existingUserOpt.get();
            } else {
                Optional<User> byEmail = userRepository.findByEmailAndDeletedFalse(email);
                if (byEmail.isPresent()) {
                    user = byEmail.get();
                    user.setGoogleId(googleId);
                    userRepository.save(user);
                } else {
                    user = new User();
                    user.setEmail(email);
                    user.setGoogleId(googleId);
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setVerified(true);
                    user.setRole(UserRole.ROLE_USER);
                    user.setPassword(null);
                    try {
                        MultipartFile avatarFile = FileUtil.downloadImageAsMultipartFile(avatarUrl, googleId);
                        String savedAvatar = fileUploadService.upload(avatarFile, "avatar");
                        user.setAvatarUrl(savedAvatar);
                    } catch (Exception e) {
                        log.error("Lỗi khi tải ảnh đại diện từ Google", e);
                    }
                    userRepository.save(user);
                }
            }

            String accessToken = jwtUtil.generateToken(user);
            String refreshToken = tokenService.createRefreshToken(user).getToken();
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            return new JwtResponse(accessToken, refreshToken);
        } catch (BusinessException e) {
            // Re-throw BusinessException (lỗi nghiệp vụ) không được nuốt
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xác thực Google token", e);
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
    }

    /**
     * Luôn trả success về phía client (không tiết lộ email có tồn tại hay không).
     */
    public void forgotPassword(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String trimmed = email.trim();
        Optional<User> optional = userRepository.findByEmailAndDeletedFalse(trimmed);
        if (optional.isEmpty()) {
            optional = userRepository.findByEmailAndDeletedFalse(trimmed.toLowerCase());
        }
        if (optional.isEmpty()) {
            log.info("[Auth] forgot-password: no user for email={}", trimmed);
            return;
        }
        User user = optional.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60));
        userRepository.save(user);

        String base = feUrl == null ? "" : feUrl.replaceAll("/+$", "");
        String resetLink = base + "/reset-password?token=" + token;
        String displayName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String html = "<p>Xin chào " + displayName + ",</p>" +
                "<p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản " + brandName + ".</p>" +
                "<p>Vui lòng nhấn vào liên kết sau để đặt mật khẩu mới:</p>" +
                "<p><a href=\"" + resetLink + "\">Đặt lại mật khẩu</a></p>" +
                "<p><i>Liên kết này sẽ hết hạn sau " + resetTokenExpiryMinutes + " phút.</i></p>" +
                "<p>Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>";
        try {
            mailService.send(user.getEmail(), "Đặt lại mật khẩu - " + brandName, html);
        } catch (Exception ex) {
            log.warn("Không thể gửi mail forgot-password tới {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    public void resetPasswordWithToken(String token, String newPassword) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
        User user = userRepository.findByResetTokenAndDeletedFalse(token.trim())
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_TOKEN));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new BusinessException(ApiCode.TOKEN_EXPIRED);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("[Auth] Password reset completed for userId={}", user.getId());
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

        mailService.send(user.getEmail(), "Gửi lại xác thực tài khoản - " + brandName, html);
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