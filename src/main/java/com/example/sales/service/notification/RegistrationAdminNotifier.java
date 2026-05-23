package com.example.sales.service.notification;

import com.example.sales.config.AppBrandProperties;
import com.example.sales.model.User;
import com.example.sales.service.MailService;
import com.example.sales.service.SupportRecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gửi email cho admin hệ thống (ROLE_ADMIN) khi có user đăng ký mới.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationAdminNotifier {

    private static final DateTimeFormatter REGISTERED_AT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MailService mailService;
    private final SupportRecipientResolver recipientResolver;
    private final AppBrandProperties appBrand;

    @Value("${app.admin.user-registration-notify.enabled:true}")
    private boolean enabled;

    /**
     * Email bổ sung (phân tách dấu phẩy), không cần ROLE_ADMIN — VD inbox vận hành.
     */
    @Value("${app.admin.user-registration-notify.extra-emails:}")
    private String extraEmails;

    @Value("${app.fe.url:}")
    private String frontendUrl;

    @Async
    public void notifyNewUserAsync(User user, RegistrationChannel channel) {
        if (!enabled || user == null || channel == null) {
            return;
        }
        try {
            Map<String, Object> baseModel = buildBaseModel(user, channel);
            String brandName = appBrand.getName();
            String subject = "[" + brandName + "] Đăng ký mới — " + user.getEmail();

            Set<String> sent = new LinkedHashSet<>();
            for (User admin : recipientResolver.getAdminUsers()) {
                if (!StringUtils.hasText(admin.getEmail())) {
                    continue;
                }
                String to = admin.getEmail().trim();
                if (!sent.add(to)) {
                    continue;
                }
                Map<String, Object> model = new HashMap<>(baseModel);
                model.put("adminName", displayName(admin));
                mailService.sendTicketEmailAsync(
                        to, subject, "emails/user-registered-admin", model);
            }

            for (String extra : parseExtraEmails()) {
                if (!sent.add(extra)) {
                    continue;
                }
                Map<String, Object> model = new HashMap<>(baseModel);
                model.put("adminName", "Admin");
                mailService.sendTicketEmailAsync(
                        extra, subject, "emails/user-registered-admin", model);
            }

            if (sent.isEmpty()) {
                log.debug("[Registration] No admin/extra emails configured — skip notify for userId={}",
                        user.getId());
            }
        } catch (Exception ex) {
            log.warn("[Registration] Failed to send admin notify for userId={}: {}",
                    user.getId(), ex.getMessage());
        }
    }

    private Map<String, Object> buildBaseModel(User user, RegistrationChannel channel) {
        String userFullName = user.getFullName();
        if (!StringUtils.hasText(userFullName)) {
            userFullName = user.getEmail();
        }
        Map<String, Object> model = new HashMap<>();
        model.put("brandName", appBrand.getName());
        model.put("userFullName", userFullName);
        model.put("userEmail", user.getEmail());
        model.put("userPhone", StringUtils.hasText(user.getPhone()) ? user.getPhone() : "—");
        model.put("countryCode", StringUtils.hasText(user.getCountryCode()) ? user.getCountryCode() : "VN");
        model.put("registrationMethodLabel", channel.getLabel());
        model.put("verificationStatus", user.isVerified() ? "Đã xác thực" : "Chờ xác thực email");
        model.put("registeredAt", formatRegisteredAt(user));
        model.put("userAdminUrl", buildAdminUserUrl(user.getId()));
        return model;
    }

    private static String displayName(User admin) {
        String name = admin.getFullName();
        return StringUtils.hasText(name) ? name : admin.getEmail();
    }

    private List<String> parseExtraEmails() {
        if (!StringUtils.hasText(extraEmails)) {
            return List.of();
        }
        return Arrays.stream(extraEmails.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String formatRegisteredAt(User user) {
        LocalDateTime at = user.getCreatedAt();
        if (at == null) {
            at = LocalDateTime.now();
        }
        return REGISTERED_AT_FMT.format(at);
    }

    private String buildAdminUserUrl(String userId) {
        String base = frontendUrl == null ? "" : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!StringUtils.hasText(userId)) {
            return base + "/admin/users";
        }
        return base + "/admin/users/" + userId;
    }

    public enum RegistrationChannel {
        EMAIL("Đăng ký email / mật khẩu"),
        GOOGLE("Đăng ký Google");

        private final String label;

        RegistrationChannel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
