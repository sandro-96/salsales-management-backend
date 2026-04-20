// File: src/main/java/com/example/sales/service/SubscriptionService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.NotificationType;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.dto.subscription.SubscriptionDto;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.repository.SubscriptionRepository;
import com.example.sales.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Nguồn sự thật cho trạng thái billing của shop (TRIAL → ACTIVE → EXPIRED).
 *
 * <p>Design notes:
 * <ul>
 *   <li>Mỗi shop có đúng 1 {@link Subscription} (ensure + lazy-create).</li>
 *   <li>Ghi {@link SubscriptionHistory} mỗi lần đổi status/thanh toán để audit.</li>
 *   <li>Không còn tier; amount cố định 99k. Nếu muốn đổi giá, chỉnh {@link #BASIC_AMOUNT_VND}
 *       hoặc đẩy vào config sau.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    public static final long TRIAL_DAYS = 30L;
    public static final long BASIC_AMOUNT_VND = 99_000L;
    private static final int BILLING_CYCLE_MONTHS = 1;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final ShopRepository shopRepository;
    private final NotificationDispatcher notificationDispatcher;

    /**
     * Lấy (hoặc tạo mới) subscription cho shop. Khi shop vừa tạo mà chưa có subscription
     * (VD migration chưa chạy), sẽ khởi tạo TRIAL từ ngày tạo shop.
     */
    public Subscription ensureSubscription(Shop shop) {
        return subscriptionRepository.findByShopId(shop.getId())
                .orElseGet(() -> startTrial(shop));
    }

    public Subscription startTrial(Shop shop) {
        LocalDateTime trialStart = shop.getCreatedAt() != null ? shop.getCreatedAt() : LocalDateTime.now();
        LocalDateTime trialEnd = trialStart.plusDays(TRIAL_DAYS);
        Subscription sub = Subscription.builder()
                .shopId(shop.getId())
                .ownerId(shop.getOwnerId())
                .status(SubscriptionStatus.TRIAL)
                .trialStartsAt(trialStart)
                .trialEndsAt(trialEnd)
                .amountVnd(BASIC_AMOUNT_VND)
                .gateway(PaymentGatewayType.MANUAL)
                .build();
        sub = subscriptionRepository.save(sub);
        log.info("[Subscription] shop {} bắt đầu TRIAL tới {}", shop.getId(), trialEnd);
        return sub;
    }

    /**
     * Ghi nhận 1 lần thanh toán thành công — kéo dài subscription thêm {@code BILLING_CYCLE_MONTHS}.
     * Nếu đang TRIAL/EXPIRED/CANCELLED: bắt đầu chu kỳ mới từ now; nếu ACTIVE còn hạn: cộng từ
     * {@code currentPeriodEnd} để không lãng phí ngày còn lại.
     */
    public Subscription recordPayment(String shopId,
                                      String transactionId,
                                      PaymentGatewayType gateway,
                                      String actorUserId) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        Subscription sub = ensureSubscription(shop);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = sub.getStatus() == SubscriptionStatus.ACTIVE
                && sub.getCurrentPeriodEnd() != null
                && sub.getCurrentPeriodEnd().isAfter(now)
                ? sub.getCurrentPeriodEnd()
                : now;
        LocalDateTime nextEnd = base.plusMonths(BILLING_CYCLE_MONTHS);

        SubscriptionStatus previousStatus = sub.getStatus();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCurrentPeriodStart(sub.getCurrentPeriodStart() == null
                || previousStatus != SubscriptionStatus.ACTIVE
                ? now : sub.getCurrentPeriodStart());
        sub.setCurrentPeriodEnd(nextEnd);
        sub.setNextBillingDate(nextEnd);
        sub.setLastPaymentAt(now);
        sub.setLastPaymentTransactionId(transactionId);
        if (gateway != null) sub.setGateway(gateway);
        sub.setAmountVnd(BASIC_AMOUNT_VND);

        subscriptionRepository.save(sub);

        SubscriptionHistory history = SubscriptionHistory.builder()
                .shopId(shopId)
                .userId(actorUserId != null ? actorUserId : shop.getOwnerId())
                .oldPlan(null)
                .newPlan(null)
                .durationMonths(BILLING_CYCLE_MONTHS)
                .transactionId(transactionId)
                .paymentMethod(gateway != null ? gateway.name() : "MANUAL")
                .actionType(SubscriptionActionType.PAYMENT)
                .build();
        historyRepository.save(history);

        notificationDispatcher.dispatch(NotificationEnvelope.builder()
                .type(NotificationType.BILLING_PAYMENT_SUCCESS)
                .shopId(shopId)
                .recipient(shop.getOwnerId())
                .title("Thanh toán thành công")
                .message("Shop \"" + shop.getName() + "\" đã được gia hạn tới "
                        + nextEnd.toLocalDate() + ".")
                .referenceId(history.getId())
                .referenceType("SUBSCRIPTION")
                .templateVar("shopName", shop.getName())
                .templateVar("amount", String.valueOf(BASIC_AMOUNT_VND))
                .templateVar("until", String.valueOf(nextEnd.toLocalDate()))
                .dedupeKey("BILLING_PAYMENT_SUCCESS:" + history.getId())
                .build());
        log.info("[Subscription] shop {} gia hạn ACTIVE tới {} (tx={}, gw={})",
                shopId, nextEnd, transactionId, gateway);
        return sub;
    }

    /** Admin gia hạn thủ công (không thanh toán) — thêm {@code months} tháng vào currentPeriodEnd. */
    public Subscription adminExtend(String shopId, int months, String reason, String adminId) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        Subscription sub = ensureSubscription(shop);
        int m = Math.max(1, months);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = sub.getStatus() == SubscriptionStatus.ACTIVE
                && sub.getCurrentPeriodEnd() != null
                && sub.getCurrentPeriodEnd().isAfter(now)
                ? sub.getCurrentPeriodEnd()
                : now;
        LocalDateTime nextEnd = base.plusMonths(m);
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            sub.setCurrentPeriodStart(now);
        }
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCurrentPeriodEnd(nextEnd);
        sub.setNextBillingDate(nextEnd);
        sub.setNote(reason);
        subscriptionRepository.save(sub);

        historyRepository.save(SubscriptionHistory.builder()
                .shopId(shopId)
                .userId(adminId)
                .durationMonths(m)
                .paymentMethod("ADMIN")
                .actionType(SubscriptionActionType.ADMIN_EXTEND)
                .build());
        log.info("[Subscription] admin {} gia hạn shop {} +{} tháng → {}", adminId, shopId, m, nextEnd);
        return sub;
    }

    /** Admin override status (VD: đánh dấu CANCELLED hoặc buộc ACTIVE). */
    public Subscription adminUpdateStatus(String shopId, SubscriptionStatus status, String reason, String adminId) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        Subscription sub = ensureSubscription(shop);
        if (status == null) throw new BusinessException(ApiCode.VALIDATION_ERROR);
        sub.setStatus(status);
        sub.setNote(reason);
        subscriptionRepository.save(sub);

        historyRepository.save(SubscriptionHistory.builder()
                .shopId(shopId)
                .userId(adminId)
                .paymentMethod("ADMIN")
                .actionType(SubscriptionActionType.ADMIN_OVERRIDE)
                .build());
        log.info("[Subscription] admin {} override shop {} status → {}", adminId, shopId, status);
        return sub;
    }

    /** Scheduler gọi để đánh dấu TRIAL/ACTIVE hết hạn. */
    public void markExpired(Subscription sub, boolean fromTrial) {
        sub.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(sub);

        historyRepository.save(SubscriptionHistory.builder()
                .shopId(sub.getShopId())
                .userId(sub.getOwnerId())
                .actionType(fromTrial ? SubscriptionActionType.TRIAL_EXPIRED
                        : SubscriptionActionType.PERIOD_EXPIRED)
                .build());

        if (sub.getOwnerId() != null) {
            notificationDispatcher.dispatch(NotificationEnvelope.builder()
                    .type(NotificationType.BILLING_PLAN_EXPIRED)
                    .shopId(sub.getShopId())
                    .recipient(sub.getOwnerId())
                    .title(fromTrial ? "Thời gian dùng thử đã kết thúc" : "Gói dịch vụ đã hết hạn")
                    .message("Vui lòng thanh toán 99.000đ để tiếp tục sử dụng đầy đủ tính năng.")
                    .referenceId(sub.getId())
                    .referenceType("SUBSCRIPTION")
                    .dedupeKey("BILLING_EXPIRED:" + sub.getShopId()
                            + ":" + LocalDateTime.now().toLocalDate())
                    .build());
        }
    }

    public SubscriptionDto toDto(Subscription sub) {
        LocalDateTime now = LocalDateTime.now();
        long trialDays = 0L;
        if (sub.getStatus() == SubscriptionStatus.TRIAL && sub.getTrialEndsAt() != null) {
            trialDays = Math.max(0L, ChronoUnit.DAYS.between(now, sub.getTrialEndsAt()));
            if (sub.getTrialEndsAt().isAfter(now)
                    && ChronoUnit.SECONDS.between(now, sub.getTrialEndsAt()) > 0) {
                trialDays = Math.max(trialDays, 1);
            }
        }
        long periodDays = 0L;
        if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getCurrentPeriodEnd() != null) {
            periodDays = Math.max(0L, ChronoUnit.DAYS.between(now, sub.getCurrentPeriodEnd()));
            if (sub.getCurrentPeriodEnd().isAfter(now)
                    && ChronoUnit.SECONDS.between(now, sub.getCurrentPeriodEnd()) > 0) {
                periodDays = Math.max(periodDays, 1);
            }
        }
        return SubscriptionDto.builder()
                .shopId(sub.getShopId())
                .status(sub.getStatus())
                .trialStartsAt(sub.getTrialStartsAt())
                .trialEndsAt(sub.getTrialEndsAt())
                .trialDaysRemaining(trialDays)
                .currentPeriodStart(sub.getCurrentPeriodStart())
                .currentPeriodEnd(sub.getCurrentPeriodEnd())
                .nextBillingDate(sub.getNextBillingDate())
                .periodDaysRemaining(periodDays)
                .amountVnd(sub.getAmountVnd())
                .gateway(sub.getGateway())
                .lastPaymentAt(sub.getLastPaymentAt())
                .lastPaymentTransactionId(sub.getLastPaymentTransactionId())
                .build();
    }

    public Subscription getByShopId(String shopId) {
        return subscriptionRepository.findByShopId(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SUBSCRIPTION_NOT_FOUND));
    }
}
