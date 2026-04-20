// File: src/main/java/com/example/sales/scheduler/PlanExpiryScheduler.java
package com.example.sales.scheduler;

import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.model.Subscription;
import com.example.sales.repository.SubscriptionRepository;
import com.example.sales.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mỗi ngày 01:00 kiểm tra:
 * <ul>
 *   <li>TRIAL có {@code trialEndsAt &lt; now} → EXPIRED (notify owner).</li>
 *   <li>ACTIVE có {@code currentPeriodEnd &lt; now} → EXPIRED (notify owner).</li>
 * </ul>
 *
 * <p>Sau khi chuyển sang mô hình Subscription, scheduler này không còn đụng
 * {@code Shop.plan}/{@code Shop.planExpiry} (legacy, giữ để rollback).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanExpiryScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 1 * * *")
    public void expireDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        List<Subscription> trialExpired = subscriptionRepository
                .findByStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIAL, now);
        for (Subscription sub : trialExpired) {
            log.info("[Scheduler] TRIAL kết thúc cho shop {} (trialEndsAt={})",
                    sub.getShopId(), sub.getTrialEndsAt());
            subscriptionService.markExpired(sub, true);
        }

        List<Subscription> activeExpired = subscriptionRepository
                .findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE, now);
        for (Subscription sub : activeExpired) {
            log.info("[Scheduler] ACTIVE hết hạn cho shop {} (periodEnd={})",
                    sub.getShopId(), sub.getCurrentPeriodEnd());
            subscriptionService.markExpired(sub, false);
        }

        if (!trialExpired.isEmpty() || !activeExpired.isEmpty()) {
            log.info("[Scheduler] đã expire {} TRIAL + {} ACTIVE",
                    trialExpired.size(), activeExpired.size());
        }
    }
}
