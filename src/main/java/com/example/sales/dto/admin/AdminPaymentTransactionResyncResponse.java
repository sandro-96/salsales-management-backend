// File: src/main/java/com/example/sales/dto/admin/AdminPaymentTransactionResyncResponse.java
package com.example.sales.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả admin resync một PaymentTransaction với gateway.
 *
 * <ul>
 *   <li>{@code gatewayStatus}: trạng thái gateway trả về — SUCCESS/FAILED/PENDING/UNKNOWN.</li>
 *   <li>{@code applied}: có apply kết quả vào DB hay không
 *       (TRUE khi SUCCESS → ghi nhận thanh toán, hoặc FAILED → đóng txn; FALSE nếu PENDING/UNKNOWN).</li>
 *   <li>{@code transaction}: snapshot txn sau khi update (chưa update nếu {@code applied=false}).</li>
 *   <li>{@code gatewayCode}/{@code gatewayMessage}: raw để admin debug.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentTransactionResyncResponse {
    private String gatewayStatus;
    private boolean applied;
    private long gatewayAmountVnd;
    private String gatewayCode;
    private String gatewayMessage;
    private AdminPaymentTransactionItem transaction;
}
