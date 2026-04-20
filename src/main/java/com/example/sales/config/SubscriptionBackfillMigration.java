// File: src/main/java/com/example/sales/config/SubscriptionBackfillMigration.java
package com.example.sales.config;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Idempotent backfill: với mỗi shop chưa có {@link Subscription}, tạo mới theo quy tắc:
 *
 * <ul>
 *   <li>Shop mới (createdAt &lt; 30 ngày trước) → TRIAL, trialEndsAt = createdAt + 30d.</li>
 *   <li>Shop đang có plan trả phí (plan != FREE) + planExpiry &gt; now → ACTIVE,
 *       currentPeriodEnd/nextBillingDate = planExpiry.</li>
 *   <li>Mọi trường hợp còn lại → EXPIRED.</li>
 * </ul>
 *
 * Migration chạy 1 lần cho mỗi shop (guard bằng {@code findByShopId}) và giữ nguyên
 * {@code Shop.plan}/{@code Shop.planExpiry} để có thể rollback nếu cần.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class SubscriptionBackfillMigration implements ApplicationRunner {

    private static final long TRIAL_DAYS = 30L;
    private static final long BASIC_AMOUNT_VND = 99_000L;

    private final ShopRepository shopRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<Shop> shops = shopRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int created = 0;

        for (Shop shop : shops) {
            if (shop.isDeleted()) continue;
            if (subscriptionRepository.findByShopId(shop.getId()).isPresent()) continue;

            Subscription sub = buildFor(shop, now);
            subscriptionRepository.save(sub);
            created++;
        }

        if (created > 0) {
            log.info("[SubscriptionBackfillMigration] đã tạo {} subscription từ shop hiện có.", created);
        }
    }

    private Subscription buildFor(Shop shop, LocalDateTime now) {
        LocalDateTime shopCreatedAt = shop.getCreatedAt() != null ? shop.getCreatedAt() : now;
        LocalDateTime trialEndsAt = shopCreatedAt.plusDays(TRIAL_DAYS);

        SubscriptionPlan plan = shop.getPlan();
        LocalDateTime planExpiry = shop.getPlanExpiry();

        Subscription.SubscriptionBuilder builder = Subscription.builder()
                .shopId(shop.getId())
                .ownerId(shop.getOwnerId())
                .trialStartsAt(shopCreatedAt)
                .trialEndsAt(trialEndsAt)
                .amountVnd(BASIC_AMOUNT_VND)
                .gateway(PaymentGatewayType.MANUAL);

        if (plan != null && plan != SubscriptionPlan.FREE
                && planExpiry != null && planExpiry.isAfter(now)) {
            builder.status(SubscriptionStatus.ACTIVE)
                    .currentPeriodStart(shopCreatedAt)
                    .currentPeriodEnd(planExpiry)
                    .nextBillingDate(planExpiry);
        } else if (trialEndsAt.isAfter(now)) {
            builder.status(SubscriptionStatus.TRIAL);
        } else {
            builder.status(SubscriptionStatus.EXPIRED);
        }
        return builder.build();
    }
}
