// File: src/main/java/com/example/sales/scheduler/PlanExpiryScheduler.java
package com.example.sales.scheduler;

import com.example.sales.constant.NotificationType;
import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.Shop;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mỗi ngày 01:00: hạ các shop quá hạn về FREE + notify owner.
 *
 * Sau migration: đẩy notification qua {@link NotificationDispatcher},
 * owner nhận cả in-app lẫn email. Dedupe theo shopId + ngày hạ gói.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanExpiryScheduler {

    private final ShopRepository shopRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Scheduled(cron = "0 0 1 * * *")
    public void downgradeExpiredPlans() {
        log.info("🔄 Kiểm tra gói đã hết hạn...");

        List<Shop> expiredShops = shopRepository.findByPlanExpiryBeforeAndPlanNot(
                LocalDateTime.now(), SubscriptionPlan.FREE);

        for (Shop shop : expiredShops) {
            SubscriptionPlan oldPlan = shop.getPlan();
            log.info("⚠️ Shop {} đã hết hạn gói {} → hạ về FREE", shop.getId(), oldPlan);

            shop.setPlan(SubscriptionPlan.FREE);
            shop.setPlanExpiry(null);

            SubscriptionHistory history = SubscriptionHistory.builder()
                    .shopId(shop.getId())
                    .userId(shop.getOwnerId())
                    .oldPlan(oldPlan)
                    .newPlan(SubscriptionPlan.FREE)
                    .durationMonths(0)
                    .paymentMethod("AUTO")
                    .actionType(SubscriptionActionType.AUTO_DOWNGRADE)
                    .build();
            historyRepository.save(history);

            if (shop.getOwnerId() != null) {
                notificationDispatcher.dispatch(NotificationEnvelope.builder()
                        .type(NotificationType.BILLING_PLAN_EXPIRED)
                        .shopId(shop.getId())
                        .recipient(shop.getOwnerId())
                        .title("Gói dịch vụ đã hết hạn")
                        .message("Shop \"" + shop.getName() + "\" đã hết hạn gói "
                                + oldPlan + " và được chuyển về FREE.")
                        .referenceId(history.getId())
                        .referenceType("SUBSCRIPTION")
                        .templateVar("shopName", shop.getName())
                        .templateVar("oldPlan", oldPlan.name())
                        .dedupeKey("BILLING_PLAN_EXPIRED:" + shop.getId()
                                + ":" + LocalDate.now())
                        .build());
            }
        }

        shopRepository.saveAll(expiredShops);
    }
}
