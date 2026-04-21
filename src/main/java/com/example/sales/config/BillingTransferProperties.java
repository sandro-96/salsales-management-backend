package com.example.sales.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Thông tin chuyển khoản cố định của hệ thống (99k / tháng) — hiển thị cho shop
 * và dùng sinh QR VietQR khi có đủ {@code vietQrBankBin} + {@code accountNumber}.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.billing.transfer")
public class BillingTransferProperties {

    /** Bật block hướng dẫn CK trên UI shop (cần ít nhất số TK + chủ TK). */
    private boolean enabled = true;

    private String bankName = "";
    private String accountNumber = "";
    private String accountHolder = "";

    /**
     * Mã BIN ngân hàng 6 số theo bảng VietQR (VD Vietcombank 970436).
     * Nếu để trống, không sinh URL QR VietQR — chỉ hiển thị số TK.
     */
    private String vietQrBankBin = "";

    /** URL ảnh QR tĩnh (tuỳ chọn) — ưu tiên hơn VietQR nếu có. */
    private String staticQrImageUrl = "";

    /**
     * Nội dung chuyển khoản. Placeholder: {@code {transactionId}}, {@code {shopId}}, {@code {shopName}}.
     * VD: {@code GD {transactionId}}
     */
    private String transferContentTemplate = "GD {transactionId}";

    public boolean hasBankAccount() {
        return notBlank(accountNumber) && notBlank(accountHolder);
    }

    public boolean isQrConfigured() {
        if (notBlank(staticQrImageUrl)) return true;
        return notBlank(vietQrBankBin) && notBlank(accountNumber);
    }

    public boolean isDisplayReady() {
        return enabled && hasBankAccount();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
