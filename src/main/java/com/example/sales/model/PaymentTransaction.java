package com.example.sales.model;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
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
 * Ghi nhận mỗi lần shop khởi tạo thanh toán qua payment gateway (VNPay/MoMo).
 * <p>
 * Mỗi record là 1 attempt: {@code providerTxnRef} là mã tham chiếu gửi sang gateway,
 * unique để đảm bảo IPN gọi lại nhiều lần vẫn idempotent (chỉ gia hạn subscription 1 lần).
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Id
    private String id;

    @Indexed
    private String shopId;
    private String ownerId;

    @Builder.Default
    private PaymentGatewayType gateway = PaymentGatewayType.MANUAL;

    /**
     * Mã tham chiếu do hệ thống sinh và gửi sang gateway
     * ({@code vnp_TxnRef} với VNPay, {@code orderId} với MoMo). Dùng để map callback ↔ attempt.
     */
    @Indexed(unique = true)
    private String providerTxnRef;

    /**
     * Mã giao dịch bên gateway trả về sau khi thành công
     * ({@code vnp_TransactionNo} với VNPay, {@code transId} với MoMo).
     */
    private String providerTransNo;

    @Builder.Default
    private long amountVnd = 0L;

    @Builder.Default
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    /** Payload gốc gửi sang gateway (serialize JSON hoặc query-string, để audit). */
    private String rawInitRequest;

    /** Payload callback/IPN gateway gửi về (để audit khi debug). */
    private String rawCallback;

    /** Mã lỗi / lý do thất bại (vnp_ResponseCode, MoMo resultCode/message). */
    private String failureReason;

    /** Thời điểm gateway xác nhận (SUCCESS/FAILED/CANCELLED). */
    private LocalDateTime completedAt;

    /** Shop chủ động báo đã chuyển khoản (chờ admin đối soát) — không thay status. */
    private LocalDateTime shopReportedTransferAt;
    private String shopReportedTransferByUserId;
}
