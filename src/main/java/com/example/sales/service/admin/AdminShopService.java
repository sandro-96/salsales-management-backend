// File: src/main/java/com/example/sales/service/admin/AdminShopService.java
package com.example.sales.service.admin;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.dto.admin.AdminShopDetail;
import com.example.sales.dto.admin.AdminShopMarkPaidRequest;
import com.example.sales.dto.admin.AdminShopPlanExtendRequest;
import com.example.sales.dto.admin.AdminShopPlanUpdateRequest;
import com.example.sales.dto.admin.AdminShopStatusUpdateRequest;
import com.example.sales.dto.admin.AdminShopSummary;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Order;
import com.example.sales.model.PaymentTransaction;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.model.User;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.PaymentTransactionRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.repository.SubscriptionRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.service.SubscriptionService;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin shop ops — giờ thao tác trên {@link Subscription} thay vì {@code Shop.plan}.
 * {@code Shop.plan}/{@code Shop.planExpiry} vẫn được giữ và update soft để rollback dễ,
 * nhưng nguồn sự thật cho status/hạn là Subscription.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminShopService {

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final ShopUserRepository shopUserRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AdminBillingService adminBillingService;

    public Page<AdminShopSummary> list(Pageable pageable, String status, SubscriptionStatus subStatus, String keyword) {
        Criteria criteria = Criteria.where("deleted").is(false);
        if ("active".equalsIgnoreCase(status)) {
            criteria = criteria.and("active").is(true);
        } else if ("locked".equalsIgnoreCase(status)) {
            criteria = criteria.and("active").is(false);
        }
        if (StringUtils.hasText(keyword)) {
            String kwRegex = ".*" + java.util.regex.Pattern.quote(keyword.trim()) + ".*";
            criteria = criteria.orOperator(
                    Criteria.where("name").regex(kwRegex, "i"),
                    Criteria.where("slug").regex(kwRegex, "i"),
                    Criteria.where("address").regex(kwRegex, "i")
            );
        }

        Query query = Query.query(criteria).with(pageable);
        List<Shop> shops;
        long total;

        if (subStatus != null) {
            // Filter by subscription status trước, dùng shopId list.
            List<Subscription> subs = subscriptionRepository.findByStatus(subStatus);
            Set<String> shopIds = subs.stream().map(Subscription::getShopId).collect(Collectors.toSet());
            if (shopIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            criteria = criteria.and("_id").in(shopIds);
            Query filtered = Query.query(criteria).with(pageable);
            total = mongoTemplate.count(Query.query(criteria), Shop.class);
            shops = mongoTemplate.find(filtered, Shop.class);
        } else {
            total = mongoTemplate.count(Query.query(criteria), Shop.class);
            shops = mongoTemplate.find(query, Shop.class);
        }

        Map<String, User> ownerMap = resolveOwners(shops);
        Map<String, Subscription> subMap = resolveSubscriptions(shops);
        List<AdminShopSummary> summaries = shops.stream()
                .map(s -> toSummary(s, ownerMap.get(s.getOwnerId()), subMap.get(s.getId())))
                .collect(Collectors.toList());
        return new PageImpl<>(summaries, pageable, total);
    }

    public AdminShopDetail detail(String shopId) {
        Shop shop = loadShop(shopId);
        User owner = userRepository.findByIdAndDeletedFalse(shop.getOwnerId()).orElse(null);
        Subscription sub = subscriptionRepository.findByShopId(shopId).orElse(null);
        AdminShopSummary summary = toSummary(shop, owner, sub);

        long branchCount = branchRepository.countByShopIdAndDeletedFalse(shopId);
        long staffCount = mongoTemplate.count(Query.query(
                Criteria.where("shopId").is(shopId).and("deleted").is(false)
        ), "shop_users");

        long totalOrders = mongoTemplate.count(Query.query(
                Criteria.where("shopId").is(shopId).and("deleted").is(false)
        ), Order.class);
        LocalDateTime last30 = LocalDateTime.now().minusDays(30);
        long ordersLast30 = mongoTemplate.count(Query.query(
                Criteria.where("shopId").is(shopId).and("deleted").is(false)
                        .and("createdAt").gte(last30)
        ), Order.class);

        List<SubscriptionHistory> history = subscriptionHistoryRepository
                .findByShopIdOrderByCreatedAtDesc(shopId);

        shopUserRepository.getClass();

        Query pendingManualQuery = Query.query(
                Criteria.where("shopId").is(shopId)
                        .and("deleted").is(false)
                        .and("gateway").is(PaymentGatewayType.MANUAL)
                        .and("status").is(PaymentTransactionStatus.PENDING)
        ).with(Sort.by(Sort.Direction.DESC, "createdAt"));
        PaymentTransaction pendingManual = mongoTemplate.findOne(pendingManualQuery, PaymentTransaction.class);
        boolean hasPendingManualBilling = pendingManual != null;
        String pendingManualProviderTxnRef = pendingManual != null ? pendingManual.getProviderTxnRef() : null;

        return AdminShopDetail.builder()
                .summary(summary)
                .branchCount(branchCount)
                .staffCount(staffCount)
                .totalOrderCount(totalOrders)
                .orderCountLast30d(ordersLast30)
                .subscriptionHistory(history)
                .hasPendingManualBilling(hasPendingManualBilling)
                .pendingManualProviderTxnRef(pendingManualProviderTxnRef)
                .build();
    }

    public AdminShopSummary updateStatus(String shopId, AdminShopStatusUpdateRequest req, String adminId) {
        Shop shop = loadShop(shopId);
        boolean newActive = Boolean.TRUE.equals(req.getActive());
        log.info("[AdminShop] {} set active={} shopId={} reason='{}'",
                adminId, newActive, shopId, req.getReason());
        shop.setActive(newActive);
        shopRepository.save(shop);
        Subscription sub = subscriptionRepository.findByShopId(shopId).orElse(null);
        return toSummary(shop, ownerOf(shop), sub);
    }

    /**
     * @deprecated UI admin mới sử dụng {@link #extendPlan} / {@link #markPaid}.
     *     Giữ để tương thích — map vào {@code subscriptionService.adminUpdateStatus}
     *     khi duration=0, hoặc {@code adminExtend} khi duration>0.
     */
    @Deprecated
    public AdminShopDetail updatePlan(String shopId, AdminShopPlanUpdateRequest req, String adminId) {
        int months = Math.max(0, req.getDurationMonths());
        if (months > 0) {
            subscriptionService.adminExtend(shopId, months, req.getReason(), adminId);
        } else {
            subscriptionService.adminUpdateStatus(shopId, SubscriptionStatus.EXPIRED, req.getReason(), adminId);
        }
        log.info("[AdminShop] {} (legacy) updatePlan shopId={} months={} reason='{}'",
                adminId, shopId, months, req.getReason());
        return detail(shopId);
    }

    public AdminShopDetail extendPlan(String shopId, AdminShopPlanExtendRequest req, String adminId) {
        subscriptionService.adminExtend(shopId, Math.max(1, req.getMonths()), req.getReason(), adminId);
        return detail(shopId);
    }

    /** Admin xác nhận thanh toán thủ công — gọi recordPayment để gia hạn 1 tháng. */
    public AdminShopDetail markPaid(String shopId, AdminShopMarkPaidRequest req, String adminId) {
        String tx = req != null && StringUtils.hasText(req.getTransactionId())
                ? req.getTransactionId() : "ADMIN-" + adminId + "-" + System.currentTimeMillis();
        PaymentGatewayType gw = req != null && req.getGateway() != null
                ? req.getGateway() : PaymentGatewayType.MANUAL;
        subscriptionService.recordPayment(shopId, tx, gw, adminId);
        paymentTransactionRepository.findByProviderTxnRef(tx).ifPresent(pt -> {
            if (pt.getGateway() == PaymentGatewayType.MANUAL
                    && pt.getStatus() == PaymentTransactionStatus.PENDING) {
                pt.setStatus(PaymentTransactionStatus.SUCCESS);
                pt.setCompletedAt(LocalDateTime.now());
                pt.setFailureReason(null);
                paymentTransactionRepository.save(pt);
            }
        });
        log.info("[AdminShop] {} markPaid shop={} tx={} gw={}", adminId, shopId, tx, gw);
        adminBillingService.invalidateOverviewCache();
        return detail(shopId);
    }

    public AdminShopDetail overrideStatus(String shopId, SubscriptionStatus status, String reason, String adminId) {
        subscriptionService.adminUpdateStatus(shopId, status, reason, adminId);
        return detail(shopId);
    }

    private Shop loadShop(String shopId) {
        Shop shop = mongoTemplate.findById(shopId, Shop.class);
        if (shop == null || shop.isDeleted()) {
            throw new BusinessException(ApiCode.NOT_FOUND);
        }
        return shop;
    }

    private User ownerOf(Shop shop) {
        return shop.getOwnerId() == null ? null
                : userRepository.findByIdAndDeletedFalse(shop.getOwnerId()).orElse(null);
    }

    private Map<String, User> resolveOwners(List<Shop> shops) {
        Set<String> ownerIds = new HashSet<>();
        for (Shop s : shops) {
            if (s.getOwnerId() != null) ownerIds.add(s.getOwnerId());
        }
        if (ownerIds.isEmpty()) return Map.of();
        List<User> owners = userRepository.findAllById(ownerIds);
        Map<String, User> map = new HashMap<>();
        for (User u : owners) {
            if (!u.isDeleted()) map.put(u.getId(), u);
        }
        return map;
    }

    private Map<String, Subscription> resolveSubscriptions(List<Shop> shops) {
        Set<String> shopIds = shops.stream().map(Shop::getId).collect(Collectors.toSet());
        if (shopIds.isEmpty()) return Map.of();
        List<Subscription> subs = mongoTemplate.find(
                Query.query(Criteria.where("shopId").in(shopIds)),
                Subscription.class
        );
        Map<String, Subscription> map = new HashMap<>();
        for (Subscription s : subs) map.put(s.getShopId(), s);
        return map;
    }

    private AdminShopSummary toSummary(Shop shop, User owner, Subscription sub) {
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = 0L;
        SubscriptionStatus status = sub != null ? sub.getStatus() : null;
        LocalDateTime trialEndsAt = sub != null ? sub.getTrialEndsAt() : null;
        LocalDateTime currentPeriodEnd = sub != null ? sub.getCurrentPeriodEnd() : null;
        LocalDateTime nextBillingDate = sub != null ? sub.getNextBillingDate() : null;
        long amount = sub != null ? sub.getAmountVnd() : SubscriptionService.BASIC_AMOUNT_VND;

        if (status == SubscriptionStatus.TRIAL && trialEndsAt != null && trialEndsAt.isAfter(now)) {
            daysRemaining = Math.max(1L, ChronoUnit.DAYS.between(now, trialEndsAt));
        } else if (status == SubscriptionStatus.ACTIVE && currentPeriodEnd != null && currentPeriodEnd.isAfter(now)) {
            daysRemaining = Math.max(1L, ChronoUnit.DAYS.between(now, currentPeriodEnd));
        }

        return AdminShopSummary.builder()
                .id(shop.getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .ownerId(shop.getOwnerId())
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerName(owner != null ? owner.getFullName() : null)
                .plan(shop.getPlan())
                .planExpiry(shop.getPlanExpiry())
                .subscriptionStatus(status)
                .trialEndsAt(trialEndsAt)
                .currentPeriodEnd(currentPeriodEnd)
                .nextBillingDate(nextBillingDate)
                .amountVnd(amount)
                .daysRemaining(daysRemaining)
                .active(shop.isActive())
                .createdAt(shop.getCreatedAt())
                .build();
    }
}
