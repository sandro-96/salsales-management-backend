// File: src/main/java/com/example/sales/service/payment/GatewayQueryResult.java
package com.example.sales.service.payment;

/**
 * Kết quả truy vấn trạng thái giao dịch từ gateway (VNPay Querydr / MoMo query).
 * <p>
 * Dùng cho flow admin <b>resync</b> một {@code PaymentTransaction} đang {@code PENDING}
 * với trạng thái thực tế ở phía gateway.
 *
 * @param valid           gateway response parse được (signature OK, reachable)
 * @param status          trạng thái gateway mapping về: {@link Status#SUCCESS} (đã thanh toán),
 *                        {@link Status#FAILED} (gateway báo thất bại/huỷ),
 *                        {@link Status#PENDING} (gateway báo vẫn đang xử lý),
 *                        {@link Status#UNKNOWN} (không tra được — network lỗi, order not found…)
 * @param providerTransNo mã giao dịch bên gateway (nếu có)
 * @param amountVnd       số tiền gateway đang ghi nhận cho txn (để đối soát)
 * @param rawCode         mã gốc ({@code vnp_ResponseCode} hoặc {@code resultCode})
 * @param message         mô tả ngắn phục vụ log/UI
 */
public record GatewayQueryResult(
        boolean valid,
        Status status,
        String providerTransNo,
        long amountVnd,
        String rawCode,
        String message
) {

    public enum Status {
        SUCCESS, FAILED, PENDING, UNKNOWN
    }

    public static GatewayQueryResult unknown(String message) {
        return new GatewayQueryResult(false, Status.UNKNOWN, null, 0L, null, message);
    }
}
