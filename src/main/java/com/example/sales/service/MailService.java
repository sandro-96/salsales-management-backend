// File: src/main/java/com/example/sales/service/MailService.java
package com.example.sales.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    public void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setText(htmlContent, true); // true = html
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(from);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không gửi được email", e);
        }
    }

    public void sendHtmlTemplate(String to, String subject, String templateName, Map<String, Object> model) {
        try {
            Context context = new Context();
            context.setVariables(model);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(from);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không gửi được email", e);
        }
    }

    /**
     * Fire-and-forget email gửi template dành cho các flow thông báo (support ticket).
     * Lỗi không làm fail HTTP request gốc — chỉ log.
     */
    @Async
    public void sendTicketEmailAsync(String to, String subject, String templateName, Map<String, Object> model) {
        try {
            sendHtmlTemplate(to, subject, templateName, model);
        } catch (Exception ex) {
            log.warn("Failed to send ticket email to {} (template={}): {}", to, templateName, ex.getMessage());
        }
    }
}
