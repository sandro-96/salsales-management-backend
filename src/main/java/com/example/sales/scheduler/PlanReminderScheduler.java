// File: com/example/sales/scheduler/PlanReminderScheduler.java
package com.example.sales.scheduler;

import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanReminderScheduler {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    // Chạy mỗi ngày lúc 7h sáng
    @Scheduled(cron = "0 0 7 * * *")
    public void remindExpiringPlans() {
        LocalDateTime targetDate = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.DAYS);

        List<Shop> shops = shopRepository.findByPlanExpiryBetween(
                targetDate,
                targetDate.plusDays(1)
        );

        for (Shop shop : shops) {
            User owner = userRepository.findById(shop.getOwnerId()).orElse(null);
            if (owner == null || owner.getEmail() == null) continue;

            Map<String, Object> model = Map.of(
                    "fullName", owner.getFullName(),
                    "shopName", shop.getName(),
                    "expiryDate", shop.getPlanExpiry().toLocalDate(),
                    "currentPlan", shop.getPlan().name()
            );

            mailService.sendHtmlTemplate(
                    owner.getEmail(),
                    "⏳ Gói " + shop.getPlan() + " sắp hết hạn",
                    "emails/plan-expiry-reminder",
                    model
            );

            log.info("📧 Đã gửi email nhắc hạn cho shop {}", shop.getName());
        }
    }
}
