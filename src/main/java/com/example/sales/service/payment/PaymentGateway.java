// File: src/main/java/com/example/sales/service/payment/PaymentGateway.java
package com.example.sales.service.payment;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.model.PaymentTransaction;

/**
 * Abstract cổng thanh toán cho subscription.
 * <p>
 * Hiện tại chỉ có các stub (MANUAL/VNPAY/MOMO) trả về URL mock + transactionId giả để phục vụ
 * luồng UX end-to-end. Khi tích hợp thật, implement mới chỉ cần trả về URL redirect tới cổng.
 */
public interface PaymentGateway {

    /** Loại gateway — dùng để chọn bean theo config {@code app.payment.gateway}. */
    PaymentGatewayType type();

    /**
     * Khởi tạo 1 giao dịch thanh toán cho subscription.
     *
     * @param request metadata đầu vào (shopId, ownerId, số tiền, returnUrl...)
     * @return thông tin payment được khởi tạo (URL redirect, transactionId...)
     */
    PaymentInitiation initiatePayment(PaymentRequest request);

    /**
     * Tra cứu trạng thái thật của một giao dịch đang lưu trong DB với gateway
     * (VNPay Querydr, MoMo query…). Dùng cho admin resync khi IPN không về kịp
     * hoặc bị mất.
     *
     * <p>Không được gây side-effect (không ghi DB, không gọi subscriptionService).
     * Caller sẽ quyết định apply kết quả như thế nào.
     *
     * @param txn bản ghi giao dịch đã lưu (có {@code providerTxnRef}, {@code createdAt}...)
     * @return kết quả chuẩn hoá; {@link GatewayQueryResult#unknown(String)} nếu gateway
     * không hỗ trợ hoặc request lỗi.
     */
    default GatewayQueryResult queryTransaction(PaymentTransaction txn) {
        return GatewayQueryResult.unknown("Gateway " + type() + " không hỗ trợ query");
    }
}
