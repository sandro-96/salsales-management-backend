package com.example.sales.scheduler;

import com.example.sales.constant.NotificationType;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.SubscriptionRepository;
import com.example.sales.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Mỗi sáng 7h gửi nhắc nhở thanh toán (T-3, T-1). Dedupe theo shop + expiry + offset.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanReminderScheduler {

    private static final int[] REMIND_DAY_OFFSETS = {3, 1};

    private final SubscriptionRepository subscriptionRepository;
    private final ShopRepository shopRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Scheduled(cron = "0 0 7 * * *")
    public void remindExpiringSubscriptions() {
        LocalDate today = LocalDate.now();

        List<Subscription> active = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        for (Subscription sub : active) {
            remindIfMatchesOffset(sub, sub.getCurrentPeriodEnd(), today, false);
        }
        List<Subscription> trials = subscriptionRepository.findByStatus(SubscriptionStatus.TRIAL);
        for (Subscription sub : trials) {
            remindIfMatchesOffset(sub, sub.getTrialEndsAt(), today, true);
        }
    }

    private void remindIfMatchesOffset(Subscription sub,
                                       LocalDateTime dueAt,
                                       LocalDate today,
                                       boolean isTrial) {
        if (dueAt == null || sub.getOwnerId() == null) return;
        LocalDate expiryDate = dueAt.toLocalDate();
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);
        int matchingOffset = matchesReminderOffset(daysUntilExpiry);
        if (matchingOffset < 0) return;

        Optional<Shop> shopOpt = shopRepository.findByIdAndDeletedFalse(sub.getShopId());
        if (shopOpt.isEmpty()) return;
        Shop shop = shopOpt.get();

        boolean isUrgent = matchingOffset == 1;
        String titleKey = resolveTitleKey(isTrial, isUrgent);
        String messageKey = resolveMessageKey(isTrial, isUrgent);

        notificationDispatcher.dispatch(NotificationEnvelope.builder()
                .type(NotificationType.BILLING_PLAN_EXPIRING_SOON)
                .shopId(sub.getShopId())
                .recipient(sub.getOwnerId())
                .referenceId(sub.getShopId())
                .referenceType("SUBSCRIPTION")
                .templateVar("titleKey", titleKey)
                .templateVar("messageKey", messageKey)
                .templateVar("shopName", shop.getName())
                .templateVar("expiryDate", expiryDate.toString())
                .templateVar("daysLeft", matchingOffset)
                .templateVar("isTrial", isTrial)
                .templateVar("isUrgent", isUrgent)
                .dedupeKey("BILLING_EXPIRING_SOON:T-" + matchingOffset + ":"
                        + sub.getShopId() + ":" + expiryDate)
                .build());
        log.info("Plan reminder shop {} (T-{} days) expiry {} ({})",
                shop.getName(), matchingOffset, expiryDate, isTrial ? "TRIAL" : "ACTIVE");
    }

    private int matchesReminderOffset(long daysUntilExpiry) {
        for (int offset : REMIND_DAY_OFFSETS) {
            if (daysUntilExpiry == offset) return offset;
        }
        return -1;
    }

    private static String resolveTitleKey(boolean isTrial, boolean isUrgent) {
        if (isTrial) {
            return isUrgent
                    ? "BILLING_PLAN_EXPIRING_SOON_TRIAL_URGENT"
                    : "BILLING_PLAN_EXPIRING_SOON_TRIAL";
        }
        return isUrgent
                ? "BILLING_PLAN_EXPIRING_SOON_ACTIVE_URGENT"
                : "BILLING_PLAN_EXPIRING_SOON_ACTIVE";
    }

    private static String resolveMessageKey(boolean isTrial, boolean isUrgent) {
        if (isTrial) {
            return isUrgent
                    ? "BILLING_PLAN_EXPIRING_SOON_TRIAL_URGENT"
                    : "BILLING_PLAN_EXPIRING_SOON_TRIAL";
        }
        return isUrgent
                ? "BILLING_PLAN_EXPIRING_SOON_ACTIVE_URGENT"
                : "BILLING_PLAN_EXPIRING_SOON_ACTIVE";
    }
}
