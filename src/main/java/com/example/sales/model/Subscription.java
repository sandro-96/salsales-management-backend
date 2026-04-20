// File: src/main/java/com/example/sales/model/Subscription.java
package com.example.sales.model;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Trạng thái billing hiện tại của 1 shop. Mỗi shop có đúng 1 document.
 * <p>
 * Đây là "source of truth" cho billing kể từ khi mô hình plan PRO/ENTERPRISE bị đơn giản hoá
 * thành TRIAL → BASIC 99k/tháng. {@code Shop.plan}/{@code Shop.planExpiry} chỉ còn giữ cho
 * tương thích ngược khi rollback migration.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "subscriptions")
public class Subscription extends BaseEntity {

    @Id
    private String id;

    /** Unique index — mỗi shop chỉ có đúng 1 subscription. */
    @Indexed(unique = true)
    private String shopId;

    /** Owner shop — snapshot để dispatch notification không cần join. */
    private String ownerId;

    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    // ─── Trial window ───────────────────────────────────────────────
    /** Thời điểm bắt đầu trial (thường = shop.createdAt). */
    private LocalDateTime trialStartsAt;
    /** Thời điểm trial kết thúc. Sau mốc này nếu không thanh toán → EXPIRED. */
    private LocalDateTime trialEndsAt;

    // ─── Current paid period (khi ACTIVE) ───────────────────────────
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    /** Ngày dự kiến charge tiếp theo — thường = currentPeriodEnd. */
    private LocalDateTime nextBillingDate;

    /** Giá mỗi chu kỳ (VND). Hiện cố định 99_000. */
    @Builder.Default
    private long amountVnd = 99_000L;

    // ─── Last payment snapshot ──────────────────────────────────────
    private LocalDateTime lastPaymentAt;
    private String lastPaymentTransactionId;

    @Builder.Default
    private PaymentGatewayType gateway = PaymentGatewayType.MANUAL;

    /** Ghi chú / lý do khi admin override status. */
    private String note;
}
