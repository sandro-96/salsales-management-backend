package com.example.sales.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.payment.vnpay")
public class VNPayProperties {

    /** Mã website của merchant cấp bởi VNPay (vnp_TmnCode). */
    private String tmnCode = "";

    /** Secret key dùng để tạo chữ ký HMAC-SHA512. Không được log ra. */
    private String hashSecret = "";

    /** URL khởi tạo thanh toán (prod vs sandbox). */
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    /**
     * Merchant Web API endpoint cho các lệnh phụ trợ ({@code querydr}, {@code refund}).
     * Prod: {@code https://pay.vnpay.vn/merchant_webapi/api/transaction}.
     */
    private String queryUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    /** URL frontend để VNPay redirect người dùng sau khi thanh toán. */
    private String returnUrl = "http://localhost:5173/billing";

    /** Số phút hiệu lực của vnp_CreateDate → vnp_ExpireDate. */
    private int expireMinutes = 15;

    public boolean isConfigured() {
        return !isBlank(tmnCode) && !isBlank(hashSecret);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
