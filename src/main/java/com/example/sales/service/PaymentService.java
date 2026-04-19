// File: src/main/java/com/example/sales/service/PaymentService.java
package com.example.sales.service;

import com.example.sales.constant.NotificationType;
import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.Shop;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionHistoryRepository historyRepository;
    private final NotificationDispatcher notificationDispatcher;

    public void upgradeShopPlan(Shop shop, SubscriptionPlan newPlan, int months) {
        SubscriptionPlan oldPlan = shop.getPlan();

        shop.setPlan(newPlan);
        shop.setPlanExpiry(LocalDateTime.now().plusMonths(months));

        SubscriptionHistory history = SubscriptionHistory.builder()
                .shopId(shop.getId())
                .userId(shop.getOwnerId())
                .oldPlan(oldPlan)
                .newPlan(newPlan)
                .durationMonths(months)
                .paymentMethod("MANUAL")
                .transactionId(null)
                .actionType(SubscriptionActionType.UPGRADE)
                .build();

        historyRepository.save(history);

        notificationDispatcher.dispatch(NotificationEnvelope.builder()
                .type(NotificationType.BILLING_PLAN_UPGRADED)
                .shopId(shop.getId())
                .recipient(shop.getOwnerId())
                .title("Gói " + newPlan.name() + " đã được kích hoạt")
                .message("Shop \"" + shop.getName() + "\" đã được nâng cấp lên "
                        + newPlan.name() + " trong " + months + " tháng.")
                .referenceId(history.getId())
                .referenceType("SUBSCRIPTION")
                .templateVar("shopName", shop.getName())
                .templateVar("oldPlan", oldPlan == null ? "" : oldPlan.name())
                .templateVar("newPlan", newPlan.name())
                .templateVar("duration", months + " tháng")
                .dedupeKey("BILLING_PLAN_UPGRADED:" + history.getId())
                .build());
    }
}
