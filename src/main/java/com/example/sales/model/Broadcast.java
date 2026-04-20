// File: src/main/java/com/example/sales/model/Broadcast.java
package com.example.sales.model;

import com.example.sales.constant.BroadcastAudience;
import com.example.sales.constant.BroadcastStatus;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Thông báo broadcast hệ thống do admin gửi tới nhóm user.
 * Chạy in-app + optional email. Snapshot lại recipient count khi gửi để audit.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("broadcasts")
public class Broadcast extends BaseEntity {

    @Id
    private String id;

    private String title;
    private String message;

    @Builder.Default
    private BroadcastAudience audience = BroadcastAudience.ALL_USERS;

    /** Dùng khi audience = SHOPS_BY_PLAN. */
    private SubscriptionPlan plan;

    /** Có gửi email song song in-app hay không. */
    @Builder.Default
    private boolean emailEnabled = false;

    @Builder.Default
    private BroadcastStatus status = BroadcastStatus.DRAFT;

    /** Snapshot tại thời điểm gửi để audit. */
    private Integer recipientCount;

    private LocalDateTime sentAt;
}
