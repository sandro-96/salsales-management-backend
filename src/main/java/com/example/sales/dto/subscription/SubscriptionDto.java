// File: src/main/java/com/example/sales/dto/subscription/SubscriptionDto.java
package com.example.sales.dto.subscription;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Trạng thái subscription hiển thị cho client (shop owner). Chỉ chứa thông tin public,
 * không expose mảng note/audit.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionDto {
    private String shopId;
    private SubscriptionStatus status;

    private LocalDateTime trialStartsAt;
    private LocalDateTime trialEndsAt;
    /** Số ngày trial còn lại (làm tròn lên). 0 khi đã hết/không ở TRIAL. */
    private long trialDaysRemaining;

    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime nextBillingDate;
    /** Số ngày còn lại tới nextBillingDate (cho ACTIVE). */
    private long periodDaysRemaining;

    private long amountVnd;
    private PaymentGatewayType gateway;

    private LocalDateTime lastPaymentAt;
    private String lastPaymentTransactionId;

    /** Mã giao dịch MANUAL đang PENDING (nếu có). */
    private String pendingManualProviderTxnRef;
    /** Thời điểm shop báo đã CK (cùng giao dịch {@link #pendingManualProviderTxnRef}). */
    private LocalDateTime pendingManualShopReportedAt;
}
