// File: src/main/java/com/example/sales/scheduler/PlanExpiryScheduler.java
package com.example.sales.scheduler;

import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.Shop;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanExpiryScheduler {

    private final ShopRepository shopRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final MailService emailService; // Giả sử bạn có một service gửi email
    private final UserRepository userRepository; // Giả sử bạn có repository để lấy thông tin người dùng

    // Chạy mỗi ngày lúc 01:00 sáng
    @Scheduled(cron = "0 0 1 * * *")
    public void downgradeExpiredPlans() {
        log.info("🔄 Kiểm tra gói đã hết hạn...");

        List<Shop> expiredShops = shopRepository.findByPlanExpiryBeforeAndPlanNot(LocalDateTime.now(), SubscriptionPlan.FREE);

        for (Shop shop : expiredShops) {
            SubscriptionPlan oldPlan = shop.getPlan();
            log.info("⚠️ Shop {} đã hết hạn gói {} → hạ về FREE", shop.getId(), shop.getPlan());
            shop.setPlan(SubscriptionPlan.FREE);
            shop.setPlanExpiry(null); // hoặc giữ nguyên nếu muốn log

            // Ghi lịch sử
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

            User owner = userRepository.findById(shop.getOwnerId()).orElse(null);
            if (owner != null && owner.getEmail() != null) {
                Map<String, Object> model = Map.of(
                        "fullName", owner.getFullName(),
                        "oldPlan", oldPlan.name(),
                        "shopName", shop.getName()
                );

                emailService.sendHtmlTemplate(
                        owner.getEmail(),
                        "Gói dịch vụ của bạn đã hết hạn",
                        "emails/plan-downgraded",
                        model
                );
            }
        }

        shopRepository.saveAll(expiredShops);
    }
}
