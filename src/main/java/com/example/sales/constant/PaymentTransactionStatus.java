package com.example.sales.constant;

/**
 * Vòng đời của một giao dịch thanh toán tại payment gateway (VNPay/MoMo).
 * Khác với {@link SubscriptionStatus}: status này chỉ mô tả lần thử thanh toán,
 * còn trạng thái gói dịch vụ của shop được lưu ở {@code Subscription}.
 */
public enum PaymentTransactionStatus {
    /** Vừa khởi tạo ở backend + redirect user sang gateway, chưa có kết quả. */
    PENDING,
    /** Gateway callback/IPN xác nhận thành công, đã gia hạn subscription. */
    SUCCESS,
    /** Gateway báo lỗi / chữ ký sai / số tiền không khớp. */
    FAILED,
    /** User huỷ trên trang gateway hoặc hết hạn timeout. */
    CANCELLED
}
