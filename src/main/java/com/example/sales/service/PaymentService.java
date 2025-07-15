// File: com/example/sales/service/PaymentService.java
package com.example.sales.service;

import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.Shop;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.model.User;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionHistoryRepository historyRepository;
    private final UserRepository userRepository; // Giả sử bạn có repository để lấy thông tin người dùng
    private final MailService mailService; // Giả sử bạn có service gửi email

    public void upgradeShopPlan(Shop shop, SubscriptionPlan newPlan, int months) {
        SubscriptionPlan oldPlan = shop.getPlan();

        shop.setPlan(newPlan);
        shop.setPlanExpiry(LocalDateTime.now().plusMonths(months));

        // Lưu lịch sử nâng cấp
        SubscriptionHistory history = SubscriptionHistory.builder()
                .shopId(shop.getId())
                .userId(shop.getOwnerId())
                .oldPlan(oldPlan)
                .newPlan(newPlan)
                .durationMonths(months)
                .paymentMethod("MANUAL") // hoặc "WEBHOOK", "STRIPE", "VNPAY", ...
                .transactionId(null) // có thể set từ webhook
                .actionType(SubscriptionActionType.UPGRADE)
                .build();

        historyRepository.save(history);

        // Gửi mail thông báo nâng cấp
        User owner = userRepository.findById(shop.getOwnerId()).orElse(null);
        if (owner != null && owner.getEmail() != null) {
            Map<String, Object> model = Map.of(
                    "fullName", owner.getFullName(),
                    "newPlan", newPlan.name(),
                    "shopName", shop.getName(),
                    "duration", months + " tháng"
            );

            mailService.sendHtmlTemplate(
                    owner.getEmail(),
                    "🎉 Gói " + newPlan.name() + " đã được kích hoạt",
                    "emails/plan-upgraded",
                    model
            );
        }
    }
}
