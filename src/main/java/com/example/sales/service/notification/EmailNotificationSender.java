package com.example.sales.service.notification;

import com.example.sales.constant.NotificationChannel;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.User;
import com.example.sales.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Channel EMAIL — render Thymeleaf template theo plan của router rồi gửi
 * qua {@link MailService#sendTicketEmailAsync} (fire-and-forget, không chặn flow).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final MailService mailService;

    @Value("${app.fe.url:}")
    private String frontendUrl;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationEnvelope envelope, User recipient, NotificationRouter.ChannelPlan plan) {
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            log.debug("Skip email for user {} (no email)", recipient.getId());
            return;
        }

        String template = plan.getEmailTemplate();
        if (template == null || template.isBlank()) {
            log.warn("No email template configured for {} — skip email for user {}",
                    envelope.getType(), recipient.getId());
            return;
        }

        String subject = plan.resolveSubject(envelope);
        Map<String, Object> model = buildModel(envelope, recipient);

        mailService.sendTicketEmailAsync(recipient.getEmail(), subject, template, model);
    }

    private Map<String, Object> buildModel(NotificationEnvelope envelope, User recipient) {
        Map<String, Object> model = new HashMap<>();
        if (envelope.getTemplateVars() != null) {
            model.putAll(envelope.getTemplateVars());
        }
        String fullName = buildFullName(recipient);
        model.putIfAbsent("recipientName", fullName);
        model.putIfAbsent("recipientEmail", recipient.getEmail());
        // Alias để giữ tương thích ngược với các template cũ (plan-upgraded,
        // plan-expiry-reminder, plan-downgraded, ...) vốn đang dùng ${fullName}.
        model.putIfAbsent("fullName", fullName);
        model.putIfAbsent("title", envelope.getTitle());
        model.putIfAbsent("message", envelope.getMessage());
        model.putIfAbsent("baseUrl", sanitizeBaseUrl());
        return model;
    }

    private String sanitizeBaseUrl() {
        if (frontendUrl == null) return "";
        return frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getLastName() != null) sb.append(user.getLastName());
        if (user.getMiddleName() != null) sb.append(" ").append(user.getMiddleName());
        if (user.getFirstName() != null) sb.append(" ").append(user.getFirstName());
        String name = sb.toString().trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
