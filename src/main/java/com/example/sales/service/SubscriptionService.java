// File: src/main/java/com/example/sales/service/SubscriptionService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.NotificationType;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.constant.UserRole;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.dto.subscription.SubscriptionDto;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.PaymentTransaction;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.model.User;
import com.example.sales.repository.PaymentTransactionRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.repository.SubscriptionRepository;
import com.example.sales.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final PaymentTransactionRepository paymentTransactionRepository;

    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                .referenceId(history.getId())
                .referenceType("SUBSCRIPTION")
                .templateVar("titleKey", "BILLING_PAYMENT_SUCCESS")
                .templateVar("messageKey", "BILLING_PAYMENT_SUCCESS")
                .templateVar("shopName", shop.getName())
                .templateVar("shopId", shop.getId())
                .templateVar("amount", String.format("%,d", BASIC_AMOUNT_VND))
                .templateVar("untilDate", nextEnd.toLocalDate().format(FMT_D))
                .templateVar("paidAt", now.format(FMT_DT))
                .templateVar("periodEndAt", nextEnd.format(FMT_DT))
                .templateVar("gateway", gateway != null ? gateway.name() : "MANUAL")
                .templateVar("transactionId", transactionId != null ? transactionId : "")
                .templateVar("subscriptionStatus", SubscriptionStatus.ACTIVE.name())
                .dedupeKey("BILLING_PAYMENT_SUCCESS:" + history.getId())
                .build());
        log.info("[Subscription] shop {} gia hạn ACTIVE tới {} (tx={}, gw={})",
                shopId, nextEnd, transactionId, gateway);
        return sub;
    }

    /**
     * Shop báo đã chuyển khoản — chỉ ghi timestamp, không đổi trạng thái giao dịch (vẫn PENDING tới khi admin xác nhận).
     */
    public void reportShopManualTransferSent(String shopId, String userId, String providerTxnRefOpt) {
        PaymentTransaction txn = resolvePendingManualTxnForShop(shopId, providerTxnRefOpt);
        if (txn == null) {
            throw new BusinessException(ApiCode.NOT_FOUND);
        }
        if (txn.getShopReportedTransferAt() != null) {
            return;
        }
        txn.setShopReportedTransferAt(LocalDateTime.now());
        txn.setShopReportedTransferByUserId(userId);
        paymentTransactionRepository.save(txn);
        shopRepository.findByIdAndDeletedFalse(shopId).ifPresent(shop ->
                notifyAdminsManualTransferPending(shop, txn.getProviderTxnRef(), txn.getAmountVnd()));
        log.info("[Subscription] shop {} báo đã CK ref={} user={}", shopId, txn.getProviderTxnRef(), userId);
    }

    /**
     * Shop huỷ yêu cầu chuyển khoản MANUAL đang PENDING (chưa được admin xác nhận SUCCESS).
     */
    public void cancelShopPendingManualTransfer(String shopId, String userId, String providerTxnRefOpt) {
        PaymentTransaction txn = resolvePendingManualTxnForShop(shopId, providerTxnRefOpt);
        if (txn == null) {
            throw new BusinessException(ApiCode.NOT_FOUND);
        }
        txn.setStatus(PaymentTransactionStatus.CANCELLED);
        txn.setFailureReason("SHOP_CANCELLED");
        txn.setCompletedAt(LocalDateTime.now());
        paymentTransactionRepository.save(txn);

        historyRepository.save(SubscriptionHistory.builder()
                .shopId(shopId)
                .userId(userId)
                .durationMonths(0)
                .transactionId(txn.getProviderTxnRef())
                .paymentMethod(PaymentGatewayType.MANUAL.name())
                .actionType(SubscriptionActionType.PAYMENT_FAILED)
                .build());
        log.info("[Subscription] shop {} huỷ MANUAL PENDING ref={} user={}",
                shopId, txn.getProviderTxnRef(), userId);

        notifyPaymentFailed(txn, "Bạn đã huỷ yêu cầu chuyển khoản. Giao dịch không được ghi nhận.");
    }

    /**
     * Gửi thông báo + email "thanh toán không thành công" cho chủ shop.
     *
     * <p>Idempotent theo {@code PaymentTransaction.id}, an toàn khi gọi lại nhiều lần
     * (ví dụ admin resync rồi lại resolve).
     */
    public void notifyPaymentFailed(PaymentTransaction txn, String reason) {
        if (txn == null) return;
        Shop shop = shopRepository.findByIdAndDeletedFalse(txn.getShopId()).orElse(null);
        if (shop == null) {
            log.warn("[Subscription] notifyPaymentFailed: shop {} không còn hoặc đã xoá — bỏ qua.",
                    txn.getShopId());
            return;
        }
        String ownerId = shop.getOwnerId();
        if (ownerId == null) {
            log.warn("[Subscription] notifyPaymentFailed: shop {} không có owner — bỏ qua.", shop.getId());
            return;
        }

        Subscription sub = subscriptionRepository.findByShopId(shop.getId()).orElse(null);
        SubscriptionStatus status = sub != null ? sub.getStatus() : null;
        String periodEndAt = sub != null && sub.getCurrentPeriodEnd() != null
                ? sub.getCurrentPeriodEnd().format(FMT_DT) : "";
        String until = sub != null && sub.getCurrentPeriodEnd() != null
                ? sub.getCurrentPeriodEnd().toLocalDate().format(FMT_D) : "";

        LocalDateTime failedAt = txn.getCompletedAt() != null
                ? txn.getCompletedAt() : LocalDateTime.now();
        String paymentResult = txn.getStatus() == PaymentTransactionStatus.CANCELLED
                ? "CANCELLED" : "FAILED";
        String safeReason = StringUtils.hasText(reason)
                ? reason
                : (StringUtils.hasText(txn.getFailureReason())
                        ? txn.getFailureReason()
                        : "");
        String txnIdForMsg = StringUtils.hasText(txn.getProviderTxnRef())
                ? txn.getProviderTxnRef() : (txn.getId() != null ? txn.getId() : "");

        notificationDispatcher.dispatch(NotificationEnvelope.builder()
                .type(NotificationType.BILLING_PAYMENT_FAILED)
                .shopId(shop.getId())
                .recipient(ownerId)
                .referenceId(txn.getId())
                .referenceType("PAYMENT_TRANSACTION")
                .templateVar("titleKey", "BILLING_PAYMENT_FAILED")
                .templateVar("messageKey", "BILLING_PAYMENT_FAILED")
                .templateVar("shopName", shop.getName())
                .templateVar("shopId", shop.getId())
                .templateVar("amount", String.format("%,d", txn.getAmountVnd()))
                .templateVar("gateway", txn.getGateway() != null ? txn.getGateway().name() : "MANUAL")
                .templateVar("transactionId", txnIdForMsg)
                .templateVar("failedAt", failedAt.format(FMT_DT))
                .templateVar("reason", safeReason)
                .templateVar("paymentResult", paymentResult)
                .templateVar("subscriptionStatus", status != null ? status.name() : "")
                .templateVar("periodEndAt", periodEndAt)
                .templateVar("until", until)
                .dedupeKey("BILLING_PAYMENT_FAILED:" + (txn.getId() != null ? txn.getId() : txnIdForMsg))
                .build());
        log.info("[Subscription] notify payment FAILED shop={} txn={} status={} reason={}",
                shop.getId(), txnIdForMsg, txn.getStatus(), safeReason);
    }

    private PaymentTransaction resolvePendingManualTxnForShop(String shopId, String providerTxnRefOpt) {
        if (StringUtils.hasText(providerTxnRefOpt)) {
            return paymentTransactionRepository.findByProviderTxnRef(providerTxnRefOpt.trim())
                    .filter(t -> shopId.equals(t.getShopId())
                            && !t.isDeleted()
                            && t.getGateway() == PaymentGatewayType.MANUAL
                            && t.getStatus() == PaymentTransactionStatus.PENDING)
                    .orElse(null);
        }
        return paymentTransactionRepository
                .findByShopIdAndStatusOrderByCreatedAtDesc(shopId, PaymentTransactionStatus.PENDING)
                .stream()
                .filter(t -> !t.isDeleted() && t.getGateway() == PaymentGatewayType.MANUAL)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gửi thông báo tới mọi user ROLE_ADMIN: có yêu cầu chuyển khoản subscription cần xác nhận.
     */
    public void notifyAdminsManualTransferPending(Shop shop, String transactionId, long amountVnd) {
        List<User> admins = userRepository.findByRoleAndDeletedFalse(UserRole.ROLE_ADMIN);
        if (admins.isEmpty()) {
            log.warn("[Subscription] không có ROLE_ADMIN — bỏ qua thông báo CK chờ shop {}", shop.getId());
            return;
        }
        String amountStr = String.format("%,d", amountVnd);

        NotificationEnvelope.NotificationEnvelopeBuilder b = NotificationEnvelope.builder()
                .type(NotificationType.BILLING_MANUAL_TRANSFER_PENDING)
                .shopId(shop.getId())
                .referenceId(transactionId)
                .referenceType("PAYMENT_TRANSACTION")
                .templateVar("titleKey", "BILLING_MANUAL_TRANSFER_PENDING")
                .templateVar("messageKey", "BILLING_MANUAL_TRANSFER_PENDING")
                .templateVar("shopName", shop.getName())
                .templateVar("shopId", shop.getId())
                .templateVar("transactionId", transactionId)
                .templateVar("amount", amountStr)
                .dedupeKey("BILLING_MANUAL_PENDING:" + transactionId);
        for (User a : admins) {
            b.recipient(a.getId());
        }
        notificationDispatcher.dispatch(b.build());
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
            String titleKey = fromTrial ? "BILLING_PLAN_EXPIRED_TRIAL" : "BILLING_PLAN_EXPIRED";
            notificationDispatcher.dispatch(NotificationEnvelope.builder()
                    .type(NotificationType.BILLING_PLAN_EXPIRED)
                    .shopId(sub.getShopId())
                    .recipient(sub.getOwnerId())
                    .referenceId(sub.getId())
                    .referenceType("SUBSCRIPTION")
                    .templateVar("titleKey", titleKey)
                    .templateVar("messageKey", "BILLING_PLAN_EXPIRED")
                    .templateVar("isTrial", fromTrial)
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
        SubscriptionDto.SubscriptionDtoBuilder b = SubscriptionDto.builder()
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
                .lastPaymentTransactionId(sub.getLastPaymentTransactionId());
        attachPendingManualForShop(b, sub.getShopId());
        return b.build();
    }

    private void attachPendingManualForShop(SubscriptionDto.SubscriptionDtoBuilder b, String shopId) {
        PaymentTransaction pending = paymentTransactionRepository
                .findByShopIdAndStatusOrderByCreatedAtDesc(shopId, PaymentTransactionStatus.PENDING)
                .stream()
                .filter(t -> !t.isDeleted() && t.getGateway() == PaymentGatewayType.MANUAL)
                .findFirst()
                .orElse(null);
        if (pending != null) {
            b.pendingManualProviderTxnRef(pending.getProviderTxnRef())
                    .pendingManualShopReportedAt(pending.getShopReportedTransferAt());
        }
    }

    public Subscription getByShopId(String shopId) {
        return subscriptionRepository.findByShopId(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SUBSCRIPTION_NOT_FOUND));
    }

    /**
     * Số ngày còn trial hoặc chu kỳ ACTIVE — dùng list shop (read-only, không ensure subscription).
     */
    public long computeDaysRemainingForList(Subscription sub) {
        LocalDateTime now = LocalDateTime.now();
        if (sub.getStatus() == SubscriptionStatus.TRIAL && sub.getTrialEndsAt() != null) {
            long trialDays = Math.max(0L, ChronoUnit.DAYS.between(now, sub.getTrialEndsAt()));
            if (sub.getTrialEndsAt().isAfter(now)
                    && ChronoUnit.SECONDS.between(now, sub.getTrialEndsAt()) > 0) {
                trialDays = Math.max(trialDays, 1L);
            }
            return trialDays;
        }
        if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getCurrentPeriodEnd() != null) {
            long periodDays = Math.max(0L, ChronoUnit.DAYS.between(now, sub.getCurrentPeriodEnd()));
            if (sub.getCurrentPeriodEnd().isAfter(now)
                    && ChronoUnit.SECONDS.between(now, sub.getCurrentPeriodEnd()) > 0) {
                periodDays = Math.max(periodDays, 1L);
            }
            return periodDays;
        }
        return 0L;
    }

    public Map<String, Subscription> mapSubscriptionsByShopId(Collection<String> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Map.of();
        }
        return subscriptionRepository.findByShopIdIn(shopIds).stream()
                .collect(Collectors.toMap(Subscription::getShopId, Function.identity(), (a, b) -> a));
    }
}
