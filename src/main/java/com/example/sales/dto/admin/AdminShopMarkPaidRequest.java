// File: src/main/java/com/example/sales/dto/admin/AdminShopMarkPaidRequest.java
package com.example.sales.dto.admin;

import com.example.sales.constant.PaymentGatewayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin đánh dấu 1 shop đã thanh toán (chuyển khoản tay, nhập tx thủ công).
 * Sẽ gọi {@code SubscriptionService.recordPayment} để gia hạn 1 chu kỳ.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminShopMarkPaidRequest {
    private String transactionId;
    private PaymentGatewayType gateway;
    private String reason;
}
