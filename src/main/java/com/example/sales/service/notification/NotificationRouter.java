package com.example.sales.service.notification;

import com.example.sales.constant.NotificationChannel;
import com.example.sales.constant.NotificationType;
import com.example.sales.dto.notification.NotificationEnvelope;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Cấu hình khai báo mapping {@link NotificationType} → tập channel + template.
 *
 * Đây là "source of truth" duy nhất về việc một sự kiện nên gửi qua kênh nào.
 * Khi cần thêm notification mới, chỉ chỉnh {@link #buildDefaultRouteTable()}
 * thay vì rải config khắp các producer.
 *
 * Sau này có thể tách ra file YAML hoặc DB-driven mà không cần đổi producer.
 */
@Component
public class NotificationRouter {

    private final Map<NotificationType, RouteConfig> table;

    public NotificationRouter() {
        this.table = buildDefaultRouteTable();
    }

    /** Trả về plan cho type+channel, hoặc null nếu channel không được bật. */
    public ChannelPlan plan(NotificationType type, NotificationChannel channel) {
        RouteConfig cfg = table.get(type);
        if (cfg == null) return null;
        return cfg.channels.get(channel);
    }

    /** Danh sách channel mặc định cho type (trước khi áp forcedChannels/preferences). */
    public Set<NotificationChannel> channelsFor(NotificationType type) {
        RouteConfig cfg = table.get(type);
        if (cfg == null) return EnumSet.noneOf(NotificationChannel.class);
        return EnumSet.copyOf(cfg.channels.keySet());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Default route table — tập trung toàn bộ policy ở đây
    // ────────────────────────────────────────────────────────────────────────

    private Map<NotificationType, RouteConfig> buildDefaultRouteTable() {
        Map<NotificationType, RouteConfig> t = new EnumMap<>(NotificationType.class);

        // Support tickets: luôn in-app; email chỉ khi HIGH/URGENT (producer hiện tại
        // đã tự lọc priority trước khi dispatch nên router bật EMAIL mặc định).
        t.put(NotificationType.TICKET_CREATED, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .channel(NotificationChannel.EMAIL,
                        ChannelPlan.email("emails/ticket-created-admin",
                                env -> "[Hỗ trợ] Yêu cầu mới — " + safe(env.getTitle())))
                .build());

        t.put(NotificationType.TICKET_REPLIED, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .channel(NotificationChannel.EMAIL,
                        ChannelPlan.email("emails/ticket-replied",
                                env -> "[Hỗ trợ] Phản hồi mới — " + safe(env.getTitle())))
                .build());

        t.put(NotificationType.TICKET_STATUS_CHANGED, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .channel(NotificationChannel.EMAIL,
                        ChannelPlan.email("emails/ticket-status-changed",
                                env -> "[Hỗ trợ] Cập nhật trạng thái — " + safe(env.getTitle())))
                .build());

        // Billing — in-app + email song song
        t.put(NotificationType.BILLING_PLAN_UPGRADED, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .channel(NotificationChannel.EMAIL,
                        ChannelPlan.email("emails/plan-upgraded",
                                env -> "🎉 Gói " + templateString(env, "newPlan")
                                        + " đã được kích hoạt"))
                .build());

        t.put(NotificationType.BILLING_PLAN_EXPIRING_SOON, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .channel(NotificationChannel.EMAIL,
                        ChannelPlan.email("emails/plan-expiry-reminder",
                                env -> "⏳ Gói " + templateString(env, "currentPlan")
                                        + " sắp hết hạn"))
                .build());

        t.put(NotificationType.BILLING_PLAN_EXPIRED, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .channel(NotificationChannel.EMAIL,
                        ChannelPlan.email("emails/plan-downgraded",
                                env -> "Gói dịch vụ của bạn đã hết hạn"))
                .build());

        // Các type tương lai (billing invoice/payment): producer chưa có, có thể
        // thêm template sau. Cho phép in-app mặc định.
        t.put(NotificationType.BILLING_INVOICE_CREATED, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .build());
        t.put(NotificationType.BILLING_PAYMENT_SUCCESS, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .build());
        t.put(NotificationType.BILLING_PAYMENT_FAILED, RouteConfig.builder()
                .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                .build());

        // Order — mặc định chỉ in-app (không spam email)
        for (NotificationType type : new NotificationType[]{
                NotificationType.ORDER_CREATED,
                NotificationType.ORDER_UPDATED,
                NotificationType.ORDER_PAID,
                NotificationType.STAFF_ADDED,
                NotificationType.STAFF_REMOVED,
                NotificationType.SYSTEM,
                NotificationType.BROADCAST,
                NotificationType.BILLING_PLAN_DOWNGRADED}) {
            t.putIfAbsent(type, RouteConfig.builder()
                    .channel(NotificationChannel.IN_APP, ChannelPlan.inApp())
                    .build());
        }

        return t;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String templateString(NotificationEnvelope env, String key) {
        Object v = env.getTemplateVars() == null ? null : env.getTemplateVars().get(key);
        return v == null ? "" : v.toString();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Config types
    // ────────────────────────────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RouteConfig {
        private final Map<NotificationChannel, ChannelPlan> channels;

        public static RouteConfigBuilder builder() {
            return new RouteConfigBuilder();
        }

        public static class RouteConfigBuilder {
            private final Map<NotificationChannel, ChannelPlan> channels =
                    new EnumMap<>(NotificationChannel.class);

            public RouteConfigBuilder channel(NotificationChannel c, ChannelPlan p) {
                this.channels.put(c, p);
                return this;
            }

            public RouteConfig build() {
                return new RouteConfig(channels);
            }
        }
    }

    /** Kế hoạch cho 1 channel cụ thể (template, subject resolver, flag, ...). */
    @Getter
    @Builder
    public static class ChannelPlan {
        private final String emailTemplate;
        private final SubjectResolver subjectResolver;

        public static ChannelPlan inApp() {
            return ChannelPlan.builder().build();
        }

        public static ChannelPlan email(String template, SubjectResolver subjectResolver) {
            return ChannelPlan.builder()
                    .emailTemplate(template)
                    .subjectResolver(subjectResolver)
                    .build();
        }

        public String resolveSubject(NotificationEnvelope envelope) {
            if (subjectResolver == null) return envelope.getTitle();
            String s = subjectResolver.resolve(envelope);
            return (s == null || s.isBlank()) ? envelope.getTitle() : s;
        }
    }

    @FunctionalInterface
    public interface SubjectResolver {
        String resolve(NotificationEnvelope envelope);
    }
}
