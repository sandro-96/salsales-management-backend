package com.example.sales.dto.subscription;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shop báo đã chuyển khoản cho giao dịch MANUAL đang chờ.
 * Nếu không gửi {@code providerTxnRef}, hệ thống gắn với giao dịch MANUAL+PENDING mới nhất của shop.
 */
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionManualTransferReportRequest {
    private String providerTxnRef;
}
