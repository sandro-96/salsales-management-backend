package com.example.sales.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Dedupe record cho notification — được chèn với key unique trước khi dispatcher
 * gọi sender. Nếu key đã tồn tại → duplicate key exception → skip.
 *
 * TTL auto-xóa bản ghi sau 30 ngày để collection không phình.
 */
@Document(collection = "notification_dedupe")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDedupe {

    @Id
    private String id;

    /**
     * Key do producer/dispatcher tính toán, ví dụ:
     *   "BILLING_PLAN_EXPIRING_SOON:shop=xyz:expiry=2026-04-22"
     * Phải là duy nhất trong phạm vi TTL.
     */
    @Indexed(unique = true)
    private String dedupeKey;

    /** TTL ~30 ngày — đủ dài cho scheduler hàng ngày, không giữ vĩnh viễn. */
    @Indexed(expireAfterSeconds = 30 * 24 * 60 * 60)
    private Instant createdAt;
}
