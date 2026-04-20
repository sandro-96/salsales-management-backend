// File: src/main/java/com/example/sales/service/admin/AdminBillingService.java
package com.example.sales.service.admin;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.dto.admin.AdminBillingOverviewResponse;
import com.example.sales.dto.admin.AdminPaymentTransactionItem;
import com.example.sales.dto.admin.AdminPaymentTransactionResyncResponse;
import com.example.sales.dto.admin.AdminSubscriptionHistoryItem;
import com.example.sales.model.PaymentTransaction;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.service.SubscriptionService;
import com.example.sales.service.payment.GatewayQueryResult;
import com.example.sales.service.payment.PaymentGateway;
import com.example.sales.service.payment.PaymentGatewayRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Thống kê billing dành cho admin theo mô hình Subscription mới.
 * <p>
 * MRR = COUNT(ACTIVE) × 99.000. Trend dựa trên {@link SubscriptionHistory} đã tồn tại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBillingService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final long CACHE_TTL_MS = 60_000L;
    private static final Set<SubscriptionActionType> RENEWAL_ACTIONS = Set.of(
            SubscriptionActionType.ADMIN_EXTEND,
            SubscriptionActionType.PAYMENT
    );
    private static final Set<SubscriptionActionType> NEW_SUB_ACTIONS = Set.of(
            SubscriptionActionType.UPGRADE,
            SubscriptionActionType.ADMIN_OVERRIDE
    );

    private final MongoTemplate mongoTemplate;
    private final SubscriptionService subscriptionService;
    private final PaymentGatewayRegistry gatewayRegistry;

    private final AtomicReference<CachedSnapshot> cache = new AtomicReference<>();

    public AdminBillingOverviewResponse overview(int trendMonths) {
        int months = Math.min(Math.max(trendMonths, 1), 24);
        CachedSnapshot snap = cache.get();
        long now = System.currentTimeMillis();
        if (snap != null && snap.months == months && now - snap.cachedAt < CACHE_TTL_MS) {
            return snap.data;
        }
        AdminBillingOverviewResponse data = compute(months);
        cache.set(new CachedSnapshot(now, months, data));
        return data;
    }

    private AdminBillingOverviewResponse compute(int months) {
        LocalDateTime nowDt = LocalDateTime.now();
        LocalDateTime in7Days = nowDt.plusDays(7);

        // Distribution theo status
        Map<SubscriptionStatus, Long> distribution = new EnumMap<>(SubscriptionStatus.class);
        for (SubscriptionStatus s : SubscriptionStatus.values()) {
            long count = mongoTemplate.count(
                    Query.query(Criteria.where("status").is(s.name())), Subscription.class);
            distribution.put(s, count);
        }

        long activePaidShops = distribution.getOrDefault(SubscriptionStatus.ACTIVE, 0L);
        long trialShops = distribution.getOrDefault(SubscriptionStatus.TRIAL, 0L);
        long expiredUnrenewed = distribution.getOrDefault(SubscriptionStatus.EXPIRED, 0L);

        long expiringSoon = mongoTemplate.count(
                Query.query(Criteria.where("status").is(SubscriptionStatus.ACTIVE.name())
                        .and("currentPeriodEnd").gt(nowDt).lte(in7Days)),
                Subscription.class);
        // Trial sắp kết thúc cũng tính
        expiringSoon += mongoTemplate.count(
                Query.query(Criteria.where("status").is(SubscriptionStatus.TRIAL.name())
                        .and("trialEndsAt").gt(nowDt).lte(in7Days)),
                Subscription.class);

        long mrr = activePaidShops * SubscriptionService.BASIC_AMOUNT_VND;

        List<YearMonth> buckets = buildMonthBuckets(months);
        LocalDateTime fromDt = buckets.get(0).atDay(1).atStartOfDay();
        List<SubscriptionHistory> histories = mongoTemplate.find(
                Query.query(Criteria.where("deleted").is(false).and("createdAt").gte(fromDt))
                        .with(Sort.by(Sort.Direction.ASC, "createdAt")),
                SubscriptionHistory.class
        );

        Map<String, Long> newSubsMap = initMonthlyBuckets(buckets);
        Map<String, Long> renewalsMap = initMonthlyBuckets(buckets);
        Map<String, Long> mrrDeltaMap = initMonthlyBuckets(buckets);

        for (SubscriptionHistory h : histories) {
            if (h.getCreatedAt() == null) continue;
            String key = YearMonth.from(h.getCreatedAt()).format(MONTH_FMT);
            SubscriptionActionType action = h.getActionType();
            if (action == null) continue;

            if (NEW_SUB_ACTIONS.contains(action)) {
                newSubsMap.computeIfPresent(key, (k, v) -> v + 1);
            } else if (RENEWAL_ACTIONS.contains(action)) {
                renewalsMap.computeIfPresent(key, (k, v) -> v + 1);
            }

            long revenue = revenueOf(h);
            if (revenue > 0) {
                mrrDeltaMap.computeIfPresent(key, (k, v) -> v + revenue);
            }
        }

        return AdminBillingOverviewResponse.builder()
                .mrrVnd(mrr)
                .activePaidShops(activePaidShops)
                .trialShops(trialShops)
                .expiringIn7Days(expiringSoon)
                .expiredUnrenewed(expiredUnrenewed)
                .subscriptionStatusDistribution(distribution)
                .mrrTrend(toMonthlyPoints(mrrDeltaMap))
                .newSubscriptions(toMonthlyPoints(newSubsMap))
                .renewals(toMonthlyPoints(renewalsMap))
                .build();
    }

    public Page<AdminSubscriptionHistoryItem> subscriptions(
            String shopId,
            SubscriptionActionType actionType,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        Criteria c = Criteria.where("deleted").is(false);
        if (StringUtils.hasText(shopId)) c.and("shopId").is(shopId);
        if (actionType != null) c.and("actionType").is(actionType.name());
        if (from != null || to != null) {
            Criteria createdAt = Criteria.where("createdAt");
            if (from != null) createdAt = createdAt.gte(from.atStartOfDay());
            if (to != null) createdAt = createdAt.lte(to.plusDays(1).atStartOfDay());
            c.andOperator(createdAt);
        }

        long total = mongoTemplate.count(Query.query(c), SubscriptionHistory.class);
        List<SubscriptionHistory> rows = mongoTemplate.find(
                Query.query(c).with(pageable).with(Sort.by(Sort.Direction.DESC, "createdAt")),
                SubscriptionHistory.class
        );

        Set<String> shopIds = rows.stream().map(SubscriptionHistory::getShopId)
                .filter(StringUtils::hasText).collect(Collectors.toCollection(HashSet::new));
        Map<String, String> shopNameById = new HashMap<>();
        if (!shopIds.isEmpty()) {
            List<Shop> shops = mongoTemplate.find(
                    Query.query(Criteria.where("_id").in(shopIds)),
                    Shop.class
            );
            for (Shop s : shops) {
                shopNameById.put(s.getId(), s.getName());
            }
        }

        List<AdminSubscriptionHistoryItem> items = rows.stream().map(h ->
                AdminSubscriptionHistoryItem.builder()
                        .id(h.getId())
                        .shopId(h.getShopId())
                        .shopName(shopNameById.get(h.getShopId()))
                        .userId(h.getUserId())
                        .oldPlan(h.getOldPlan())
                        .newPlan(h.getNewPlan())
                        .durationMonths(h.getDurationMonths())
                        .transactionId(h.getTransactionId())
                        .paymentMethod(h.getPaymentMethod())
                        .actionType(h.getActionType())
                        .createdAt(h.getCreatedAt())
                        .build()
        ).toList();

        return new PageImpl<>(items, pageable, total);
    }

    /**
     * Danh sách các lần khởi tạo thanh toán (PaymentTransaction) dành cho admin.
     * Khác với {@code subscriptions(...)} (history cấp business) — endpoint này
     * trả về trace cấp gateway, giúp admin debug IPN thất bại / chữ ký sai / amount mismatch.
     */
    public Page<AdminPaymentTransactionItem> paymentTransactions(
            String shopId,
            PaymentTransactionStatus status,
            PaymentGatewayType gateway,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        Criteria c = Criteria.where("deleted").is(false);
        if (StringUtils.hasText(shopId)) c = c.and("shopId").is(shopId);
        if (status != null) c = c.and("status").is(status.name());
        if (gateway != null) c = c.and("gateway").is(gateway.name());
        if (from != null || to != null) {
            Criteria createdAt = Criteria.where("createdAt");
            if (from != null) createdAt = createdAt.gte(from.atStartOfDay());
            if (to != null) createdAt = createdAt.lte(to.plusDays(1).atStartOfDay());
            c = c.andOperator(createdAt);
        }

        long total = mongoTemplate.count(Query.query(c), PaymentTransaction.class);
        List<PaymentTransaction> rows = mongoTemplate.find(
                Query.query(c).with(pageable).with(Sort.by(Sort.Direction.DESC, "createdAt")),
                PaymentTransaction.class
        );

        Set<String> shopIds = rows.stream().map(PaymentTransaction::getShopId)
                .filter(StringUtils::hasText).collect(Collectors.toCollection(HashSet::new));
        Map<String, String> shopNameById = new HashMap<>();
        if (!shopIds.isEmpty()) {
            List<Shop> shops = mongoTemplate.find(
                    Query.query(Criteria.where("_id").in(shopIds)),
                    Shop.class
            );
            for (Shop s : shops) shopNameById.put(s.getId(), s.getName());
        }

        List<AdminPaymentTransactionItem> items = rows.stream().map(t ->
                AdminPaymentTransactionItem.builder()
                        .id(t.getId())
                        .shopId(t.getShopId())
                        .shopName(shopNameById.get(t.getShopId()))
                        .ownerId(t.getOwnerId())
                        .gateway(t.getGateway())
                        .providerTxnRef(t.getProviderTxnRef())
                        .providerTransNo(t.getProviderTransNo())
                        .amountVnd(t.getAmountVnd())
                        .status(t.getStatus())
                        .failureReason(t.getFailureReason())
                        .createdAt(t.getCreatedAt())
                        .completedAt(t.getCompletedAt())
                        .build()
        ).toList();

        return new PageImpl<>(items, pageable, total);
    }

    /**
     * Đánh dấu một PaymentTransaction là đã giải quyết thủ công bởi admin.
     * <p>
     * Chỉ cho phép chuyển từ {@link PaymentTransactionStatus#PENDING} →
     * {@link PaymentTransactionStatus#CANCELLED} hoặc {@link PaymentTransactionStatus#FAILED}.
     * <p>
     * <b>Không bao giờ</b> cho phép set SUCCESS thủ công — nếu gateway đã confirm thì IPN sẽ tự chạy;
     * nếu admin cần bù tiền thì dùng {@code POST /api/admin/shops/{id}/plan:extend} thay vì đổi txn trực tiếp
     * (tránh lệch đối soát gateway).
     *
     * @param txnId           id PaymentTransaction
     * @param newStatus       CANCELLED hoặc FAILED
     * @param reason          lý do (bắt buộc)
     * @return item sau khi update
     */
    public AdminPaymentTransactionItem resolvePaymentTransaction(
            String txnId,
            PaymentTransactionStatus newStatus,
            String reason
    ) {
        if (newStatus == null
                || (newStatus != PaymentTransactionStatus.CANCELLED
                    && newStatus != PaymentTransactionStatus.FAILED)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        PaymentTransaction txn = mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(txnId).and("deleted").is(false)),
                PaymentTransaction.class
        );
        if (txn == null) {
            throw new BusinessException(ApiCode.NOT_FOUND);
        }
        if (txn.getStatus() != PaymentTransactionStatus.PENDING) {
            // Idempotency: chỉ resolve PENDING. SUCCESS/CANCELLED/FAILED đã final.
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        txn.setStatus(newStatus);
        txn.setFailureReason(reason);
        txn.setCompletedAt(LocalDateTime.now());
        PaymentTransaction saved = mongoTemplate.save(txn);
        log.info("[AdminBilling] Admin resolved txn id={} ref={} → {} reason={}",
                saved.getId(), saved.getProviderTxnRef(), newStatus, reason);

        String shopName = null;
        if (StringUtils.hasText(saved.getShopId())) {
            Shop shop = mongoTemplate.findOne(
                    Query.query(Criteria.where("_id").is(saved.getShopId())),
                    Shop.class
            );
            if (shop != null) shopName = shop.getName();
        }
        return AdminPaymentTransactionItem.builder()
                .id(saved.getId())
                .shopId(saved.getShopId())
                .shopName(shopName)
                .ownerId(saved.getOwnerId())
                .gateway(saved.getGateway())
                .providerTxnRef(saved.getProviderTxnRef())
                .providerTransNo(saved.getProviderTransNo())
                .amountVnd(saved.getAmountVnd())
                .status(saved.getStatus())
                .failureReason(saved.getFailureReason())
                .createdAt(saved.getCreatedAt())
                .completedAt(saved.getCompletedAt())
                .build();
    }

    /**
     * Gọi gateway (VNPay Querydr / MoMo query) để xác định trạng thái thực tế
     * của một {@link PaymentTransaction} đang PENDING và đồng bộ về DB.
     *
     * <p>Luồng:
     * <ol>
     *   <li>Không re-query với gateway MANUAL (không có API) và với txn đã final
     *       (SUCCESS/CANCELLED/FAILED) — trả snapshot nguyên trạng.</li>
     *   <li>Gọi {@link PaymentGateway#queryTransaction(PaymentTransaction)}.</li>
     *   <li>Nếu gateway báo SUCCESS: kiểm tra amount match, gọi
     *       {@link SubscriptionService#recordPayment(String, String, com.example.sales.constant.PaymentGatewayType, Integer)}
     *       (idempotent nhờ {@code providerTxnRef} unique), rồi mark SUCCESS.</li>
     *   <li>Nếu gateway báo FAILED: mark FAILED với lý do.</li>
     *   <li>Nếu PENDING/UNKNOWN: không ghi DB, chỉ trả kết quả để admin xem.</li>
     * </ol>
     *
     * <p>Không dùng @Transactional để giữ nguyên hành vi {@link com.example.sales.controller.WebhookController}:
     * nếu {@code recordPayment} ném {@code BusinessException} (ví dụ shop đã xoá) thì
     * txn sẽ được mark FAILED thay vì rollback cả save.
     */
    public AdminPaymentTransactionResyncResponse resyncPaymentTransaction(String txnId) {
        if (!StringUtils.hasText(txnId)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        PaymentTransaction txn = mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(txnId).and("deleted").is(false)),
                PaymentTransaction.class
        );
        if (txn == null) {
            throw new BusinessException(ApiCode.NOT_FOUND);
        }

        // Không resync với txn đã final hoặc gateway không hỗ trợ query (MANUAL).
        if (txn.getStatus() != PaymentTransactionStatus.PENDING) {
            return AdminPaymentTransactionResyncResponse.builder()
                    .gatewayStatus(GatewayQueryResult.Status.UNKNOWN.name())
                    .applied(false)
                    .gatewayMessage("Txn đã final: " + txn.getStatus() + " — không resync")
                    .transaction(toItem(txn))
                    .build();
        }
        if (txn.getGateway() == null || txn.getGateway() == PaymentGatewayType.MANUAL) {
            return AdminPaymentTransactionResyncResponse.builder()
                    .gatewayStatus(GatewayQueryResult.Status.UNKNOWN.name())
                    .applied(false)
                    .gatewayMessage("Gateway MANUAL không có API query — dùng resolve thủ công")
                    .transaction(toItem(txn))
                    .build();
        }

        PaymentGateway gw = gatewayRegistry.resolve(txn.getGateway());
        GatewayQueryResult result = gw.queryTransaction(txn);
        log.info("[AdminBilling] Resync txn id={} ref={} → gatewayStatus={} code={} msg={}",
                txn.getId(), txn.getProviderTxnRef(), result.status(),
                result.rawCode(), result.message());

        boolean applied = false;
        switch (result.status()) {
            case SUCCESS -> {
                // Check amount match trước khi confirm.
                if (result.amountVnd() > 0 && txn.getAmountVnd() != result.amountVnd()) {
                    txn.setStatus(PaymentTransactionStatus.FAILED);
                    txn.setFailureReason("resync amount mismatch: expected="
                            + txn.getAmountVnd() + " gateway=" + result.amountVnd());
                    txn.setCompletedAt(LocalDateTime.now());
                    mongoTemplate.save(txn);
                    applied = true;
                    break;
                }
                try {
                    subscriptionService.recordPayment(
                            txn.getShopId(),
                            result.providerTransNo() != null
                                    ? result.providerTransNo() : txn.getProviderTxnRef(),
                            txn.getGateway(),
                            null);
                    txn.setStatus(PaymentTransactionStatus.SUCCESS);
                    txn.setProviderTransNo(result.providerTransNo());
                    txn.setFailureReason(null);
                    txn.setCompletedAt(LocalDateTime.now());
                    mongoTemplate.save(txn);
                    applied = true;
                } catch (BusinessException ex) {
                    txn.setStatus(PaymentTransactionStatus.FAILED);
                    txn.setFailureReason("resync recordPayment failed: "
                            + ex.getError() + " " + ex.getMessage());
                    txn.setCompletedAt(LocalDateTime.now());
                    mongoTemplate.save(txn);
                    applied = true;
                }
            }
            case FAILED -> {
                txn.setStatus(PaymentTransactionStatus.FAILED);
                txn.setFailureReason("resync: " + result.message());
                txn.setCompletedAt(LocalDateTime.now());
                mongoTemplate.save(txn);
                applied = true;
            }
            case PENDING, UNKNOWN -> {
                // Không update DB — admin có thể resync lại sau hoặc dùng resolve thủ công.
            }
        }

        return AdminPaymentTransactionResyncResponse.builder()
                .gatewayStatus(result.status().name())
                .applied(applied)
                .gatewayAmountVnd(result.amountVnd())
                .gatewayCode(result.rawCode())
                .gatewayMessage(result.message())
                .transaction(toItem(txn))
                .build();
    }

    private AdminPaymentTransactionItem toItem(PaymentTransaction t) {
        String shopName = null;
        if (StringUtils.hasText(t.getShopId())) {
            Shop shop = mongoTemplate.findOne(
                    Query.query(Criteria.where("_id").is(t.getShopId())),
                    Shop.class
            );
            if (shop != null) shopName = shop.getName();
        }
        return AdminPaymentTransactionItem.builder()
                .id(t.getId())
                .shopId(t.getShopId())
                .shopName(shopName)
                .ownerId(t.getOwnerId())
                .gateway(t.getGateway())
                .providerTxnRef(t.getProviderTxnRef())
                .providerTransNo(t.getProviderTransNo())
                .amountVnd(t.getAmountVnd())
                .status(t.getStatus())
                .failureReason(t.getFailureReason())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }

    private long revenueOf(SubscriptionHistory h) {
        SubscriptionActionType action = h.getActionType();
        if (action == SubscriptionActionType.PAYMENT
                || action == SubscriptionActionType.ADMIN_EXTEND) {
            int dur = Math.max(1, h.getDurationMonths());
            return SubscriptionService.BASIC_AMOUNT_VND * dur;
        }
        return 0L;
    }

    private List<YearMonth> buildMonthBuckets(int months) {
        YearMonth end = YearMonth.now();
        List<YearMonth> result = new ArrayList<>(months);
        for (int i = months - 1; i >= 0; i--) {
            result.add(end.minusMonths(i));
        }
        return result;
    }

    private Map<String, Long> initMonthlyBuckets(List<YearMonth> months) {
        Map<String, Long> map = new TreeMap<>();
        for (YearMonth ym : months) {
            map.put(ym.format(MONTH_FMT), 0L);
        }
        return map;
    }

    private List<AdminBillingOverviewResponse.MonthlyPoint> toMonthlyPoints(Map<String, Long> m) {
        return m.entrySet().stream()
                .map(e -> AdminBillingOverviewResponse.MonthlyPoint.builder()
                        .month(e.getKey()).value(e.getValue()).build())
                .toList();
    }

    private record CachedSnapshot(long cachedAt, int months, AdminBillingOverviewResponse data) {}
}
