package com.example.sales.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.payment.momo")
public class MomoProperties {

    private String partnerCode = "";
    private String accessKey = "";
    private String secretKey = "";

    /** Base URL của MoMo API (sandbox vs prod). POST /create sẽ được append. */
    private String endpoint = "https://test-payment.momo.vn/v2/gateway/api";

    /** URL frontend MoMo redirect người dùng sau khi thanh toán. */
    private String redirectUrl = "http://localhost:5173/billing";

    /** URL backend để MoMo gọi IPN (server-to-server). */
    private String ipnUrl = "http://localhost:8080/api/webhook/momo";

    /** Loại giao dịch — "captureWallet" là cho ví MoMo (phổ biến cho one-shot). */
    private String requestType = "captureWallet";

    public boolean isConfigured() {
        return !isBlank(partnerCode) && !isBlank(accessKey) && !isBlank(secretKey);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
