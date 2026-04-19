// File: src/main/java/com/example/sales/scheduler/PlanReminderScheduler.java
package com.example.sales.scheduler;

import com.example.sales.constant.NotificationType;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.Shop;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Mỗi sáng 7h: nhắc các shop có gói hết hạn trong 3 ngày nữa.
 *
 * Sau migration: thay vì gọi MailService trực tiếp, scheduler build
 * {@link NotificationEnvelope} và giao cho {@link NotificationDispatcher}.
 * Dedupe key theo (shopId + ngày hết hạn) đảm bảo nếu scheduler lỡ chạy
 * lại trong cùng ngày (restart, retry) cũng không gửi trùng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanReminderScheduler {

    private final ShopRepository shopRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Scheduled(cron = "0 0 7 * * *")
    public void remindExpiringPlans() {
        LocalDateTime targetDate = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.DAYS);

        List<Shop> shops = shopRepository.findByPlanExpiryBetween(
                targetDate,
                targetDate.plusDays(1)
        );

        for (Shop shop : shops) {
            if (shop.getOwnerId() == null) continue;

            LocalDate expiryDate = shop.getPlanExpiry().toLocalDate();

            notificationDispatcher.dispatch(NotificationEnvelope.builder()
                    .type(NotificationType.BILLING_PLAN_EXPIRING_SOON)
                    .shopId(shop.getId())
                    .recipient(shop.getOwnerId())
                    .title("Gói " + shop.getPlan() + " sắp hết hạn")
                    .message("Gói " + shop.getPlan() + " của shop \"" + shop.getName()
                            + "\" sẽ hết hạn vào " + expiryDate)
                    .referenceId(shop.getId())
                    .referenceType("SUBSCRIPTION")
                    .templateVar("shopName", shop.getName())
                    .templateVar("currentPlan", shop.getPlan().name())
                    .templateVar("expiryDate", expiryDate)
                    .dedupeKey("BILLING_PLAN_EXPIRING_SOON:" + shop.getId() + ":" + expiryDate)
                    .build());

            log.info("📧 Dispatched plan-expiring-soon for shop {}", shop.getName());
        }
    }
}
