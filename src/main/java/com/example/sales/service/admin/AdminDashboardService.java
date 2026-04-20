// File: src/main/java/com/example/sales/service/admin/AdminDashboardService.java
package com.example.sales.service.admin;

import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.constant.TicketPriority;
import com.example.sales.constant.TicketStatus;
import com.example.sales.constant.UserRole;
import com.example.sales.dto.admin.AdminDashboardResponse;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.model.SupportTicket;
import com.example.sales.model.User;
import com.example.sales.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tính KPI tổng quan cho admin dashboard. Dùng {@link MongoTemplate} để count/aggregate
 * trực tiếp, tránh load toàn bộ collection.
 *
 * <p>Cache nhẹ in-memory 60s: giảm tải khi nhiều admin cùng mở dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final long CACHE_TTL_MS = 60_000L;

    private final MongoTemplate mongoTemplate;

    private final AtomicReference<CachedSnapshot> cache = new AtomicReference<>();

    public AdminDashboardResponse overview() {
        CachedSnapshot snapshot = cache.get();
        long now = System.currentTimeMillis();
        if (snapshot != null && (now - snapshot.cachedAt) < CACHE_TTL_MS) {
            return snapshot.data;
        }
        AdminDashboardResponse data = computeOverview();
        cache.set(new CachedSnapshot(now, data));
        return data;
    }

    private AdminDashboardResponse computeOverview() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime last30 = LocalDate.now().minusDays(29).atStartOfDay();

        long totalShops = count(Shop.class, Criteria.where("deleted").is(false));
        long activeShops = count(Shop.class, Criteria.where("deleted").is(false)
                .and("active").is(true));
        long newShopsThisMonth = count(Shop.class, Criteria.where("deleted").is(false)
                .and("createdAt").gte(startOfMonth));

        long totalUsers = count(User.class, Criteria.where("deleted").is(false));
        long newUsersThisMonth = count(User.class, Criteria.where("deleted").is(false)
                .and("createdAt").gte(startOfMonth));
        long totalAdmins = count(User.class, Criteria.where("deleted").is(false)
                .and("role").is(UserRole.ROLE_ADMIN.name()));

        Map<SubscriptionStatus, Long> subscriptionDistribution = new EnumMap<>(SubscriptionStatus.class);
        for (SubscriptionStatus s : SubscriptionStatus.values()) {
            subscriptionDistribution.put(s,
                    count(Subscription.class, Criteria.where("status").is(s.name())));
        }

        long openTickets = count(SupportTicket.class, Criteria.where("deleted").is(false)
                .and("status").in(TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name()));
        long urgentTickets = count(SupportTicket.class, Criteria.where("deleted").is(false)
                .and("status").in(TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name())
                .and("priority").in(TicketPriority.HIGH.name(), TicketPriority.URGENT.name()));

        long mrrVnd = subscriptionDistribution.getOrDefault(SubscriptionStatus.ACTIVE, 0L)
                * SubscriptionService.BASIC_AMOUNT_VND;

        List<AdminDashboardResponse.DailyPoint> shopsTrend = dailyTrend(Shop.class, last30);
        List<AdminDashboardResponse.DailyPoint> usersTrend = dailyTrend(User.class, last30);

        return AdminDashboardResponse.builder()
                .totalShops(totalShops)
                .activeShops(activeShops)
                .newShopsThisMonth(newShopsThisMonth)
                .totalUsers(totalUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .totalAdmins(totalAdmins)
                .subscriptionStatusDistribution(subscriptionDistribution)
                .openTickets(openTickets)
                .urgentTickets(urgentTickets)
                .mrrVnd(mrrVnd)
                .newShopsTrend(shopsTrend)
                .newUsersTrend(usersTrend)
                .build();
    }

    private <T> long count(Class<T> entity, Criteria criteria) {
        return mongoTemplate.count(Query.query(criteria), entity);
    }

    private <T> List<AdminDashboardResponse.DailyPoint> dailyTrend(Class<T> entity, LocalDateTime since) {
        Query q = Query.query(Criteria.where("deleted").is(false).and("createdAt").gte(since));
        q.fields().include("createdAt");
        List<T> docs = mongoTemplate.find(q, entity);
        TreeMap<String, Long> buckets = new TreeMap<>();
        // khởi tạo đủ 30 điểm để biểu đồ liên tục
        LocalDate start = since.toLocalDate();
        for (int i = 0; i < 30; i++) {
            buckets.put(start.plusDays(i).format(DAY_FMT), 0L);
        }
        for (T doc : docs) {
            LocalDateTime createdAt = extractCreatedAt(doc);
            if (createdAt == null) continue;
            String key = createdAt.toLocalDate().format(DAY_FMT);
            buckets.computeIfPresent(key, (k, v) -> v + 1);
        }
        List<AdminDashboardResponse.DailyPoint> points = new ArrayList<>();
        for (Map.Entry<String, Long> e : buckets.entrySet()) {
            points.add(AdminDashboardResponse.DailyPoint.builder()
                    .date(e.getKey()).count(e.getValue()).build());
        }
        return points;
    }

    private LocalDateTime extractCreatedAt(Object doc) {
        if (doc instanceof Shop s) return s.getCreatedAt();
        if (doc instanceof User u) return u.getCreatedAt();
        return null;
    }

    private record CachedSnapshot(long cachedAt, AdminDashboardResponse data) {
    }
}
