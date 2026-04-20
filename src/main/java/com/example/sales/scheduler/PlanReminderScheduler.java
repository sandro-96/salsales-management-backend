// File: src/main/java/com/example/sales/scheduler/PlanReminderScheduler.java
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
 * Mỗi sáng 7h gửi nhắc nhở thanh toán:
 * - T-3 ngày: cảnh báo nhẹ
 * - T-1 ngày: cảnh báo khẩn
 * <p>
 * Dedupe theo (type + shopId + expiryDate) nên mỗi mốc chỉ gửi 1 lần cho cùng kỳ.
 * Áp dụng cho subscription ACTIVE (hết {@code currentPeriodEnd}) và TRIAL ({@code trialEndsAt}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanReminderScheduler {

    private static final int[] REMIND_DAY_OFFSETS = { 3, 1 };

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
        String title = buildTitle(isTrial, isUrgent);
        String message = buildMessage(shop.getName(), expiryDate, isTrial, isUrgent, matchingOffset);

        notificationDispatcher.dispatch(NotificationEnvelope.builder()
                .type(NotificationType.BILLING_PLAN_EXPIRING_SOON)
                .shopId(sub.getShopId())
                .recipient(sub.getOwnerId())
                .title(title)
                .message(message)
                .referenceId(sub.getShopId())
                .referenceType("SUBSCRIPTION")
                .templateVar("shopName", shop.getName())
                .templateVar("expiryDate", expiryDate)
                .templateVar("daysLeft", matchingOffset)
                .templateVar("isTrial", isTrial)
                .dedupeKey("BILLING_EXPIRING_SOON:T-" + matchingOffset + ":"
                        + sub.getShopId() + ":" + expiryDate)
                .build());
        log.info("📧 Nhắc shop {} (T-{} ngày) hết hạn {} ({})",
                shop.getName(), matchingOffset, expiryDate, isTrial ? "TRIAL" : "ACTIVE");
    }

    private int matchesReminderOffset(long daysUntilExpiry) {
        for (int offset : REMIND_DAY_OFFSETS) {
            if (daysUntilExpiry == offset) return offset;
        }
        return -1;
    }

    private String buildTitle(boolean isTrial, boolean isUrgent) {
        if (isTrial) {
            return isUrgent
                    ? "⚠️ Thời gian dùng thử kết thúc ngày mai"
                    : "Thời gian dùng thử sắp kết thúc";
        }
        return isUrgent
                ? "⚠️ Gói dịch vụ hết hạn ngày mai"
                : "Gói dịch vụ sắp hết hạn";
    }

    private String buildMessage(String shopName, LocalDate expiryDate,
                                boolean isTrial, boolean isUrgent, int daysLeft) {
        String prefix = isUrgent
                ? "Shop \"" + shopName + "\" sẽ hết hạn vào " + expiryDate + " (còn 1 ngày)."
                : "Shop \"" + shopName + "\" sẽ hết hạn vào " + expiryDate
                        + " (còn " + daysLeft + " ngày).";
        String suffix = isTrial
                ? " Vui lòng thanh toán 99.000đ để tiếp tục sử dụng sau khi hết trial."
                : " Vui lòng thanh toán 99.000đ để gia hạn và tránh gián đoạn dịch vụ.";
        return prefix + suffix;
    }
}
