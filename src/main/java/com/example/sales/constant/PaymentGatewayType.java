// File: src/main/java/com/example/sales/constant/PaymentGatewayType.java
package com.example.sales.constant;

/**
 * Cổng thanh toán áp dụng cho các giao dịch gia hạn gói shop.
 * <p>
 * {@code MANUAL} dùng cho thao tác xác nhận thủ công của admin (chuyển khoản trực tiếp)
 * hoặc migration cũ. {@code VNPAY}/{@code MOMO} là các stub placeholder, sẵn sàng thay
 * bằng cổng thật về sau.
 */
public enum PaymentGatewayType {
    MANUAL,
    VNPAY,
    MOMO
}
