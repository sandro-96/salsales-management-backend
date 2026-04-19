package com.example.sales.dto.notification;

import com.example.sales.constant.NotificationChannel;
import com.example.sales.constant.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Envelope bất biến (immutable) mà các producer (service/scheduler) gửi vào
 * {@link com.example.sales.service.notification.NotificationDispatcher}.
 *
 * Producer không cần biết sẽ đi qua những channel nào — chỉ mô tả "ai nhận",
 * "sự kiện gì", và dữ liệu kèm theo. Việc routing sang IN_APP / EMAIL / ...
 * được quyết định bởi {@link com.example.sales.service.notification.NotificationRouter}.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class NotificationEnvelope {

    /** Loại sự kiện — dùng để tra router ra channel + template. */
    private final NotificationType type;

    /** Shop context (có thể null cho notification cấp hệ thống). */
    private final String shopId;

    /** Danh sách userId nhận. Hỗ trợ broadcast tới nhiều người. */
    @Singular("recipient")
    private final List<String> recipientUserIds;

    /** Tiêu đề in-app / subject email (có thể bị override bởi template). */
    private final String title;

    /** Nội dung rút gọn cho in-app / SMS. */
    private final String message;

    /** Reference tới entity gốc (ticketId, invoiceId, ...) — để FE deep-link. */
    private final String referenceId;

    /** Loại reference (TICKET, INVOICE, SUBSCRIPTION, ...). */
    private final String referenceType;

    /** Người thực hiện hành động (nullable, dùng cho hiển thị "X đã phản hồi"). */
    private final String actorId;
    private final String actorName;

    /**
     * Dữ liệu để render email/push template. Dispatcher sẽ bổ sung các biến
     * hệ thống (baseUrl, recipient.name, ...) trước khi truyền cho sender.
     */
    @Singular
    private final Map<String, Object> templateVars;

    /**
     * Channel override — nếu set sẽ bỏ qua cấu hình mặc định của router.
     * Ví dụ: producer force gửi email bất kể preference (dùng cho critical alerts).
     */
    @Singular("forceChannel")
    private final Set<NotificationChannel> forcedChannels;

    /**
     * Khóa idempotency (nullable). Khi được cung cấp, dispatcher sẽ bỏ qua
     * nếu đã gửi envelope cùng key (trong TTL) — tránh double-send cho
     * scheduler chạy đi chạy lại.
     */
    private final String dedupeKey;
}
