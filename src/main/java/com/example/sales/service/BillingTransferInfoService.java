package com.example.sales.service;

import com.example.sales.config.BillingTransferProperties;
import com.example.sales.dto.subscription.SubscriptionTransferInstructionsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Build hướng dẫn chuyển khoản + URL QR (VietQR / static) từ {@link BillingTransferProperties}.
 */
@Service
@RequiredArgsConstructor
public class BillingTransferInfoService {

    private final BillingTransferProperties props;

    /**
     * Thông tin tĩnh (không có mã giao dịch) — dùng cho GET /transfer-info.
     */
    public SubscriptionTransferInstructionsDto buildStaticPreview(long amountVnd) {
        if (!props.isDisplayReady()) {
            return null;
        }
        return SubscriptionTransferInstructionsDto.builder()
                .bankName(trim(props.getBankName()))
                .accountNumber(trim(props.getAccountNumber()))
                .accountHolder(trim(props.getAccountHolder()))
                .amountVnd(amountVnd)
                .transferContent(null)
                .qrImageUrl(resolveStaticQrOnly())
                .instructions(buildStaticInstructions(amountVnd))
                .build();
    }

    /**
     * Đầy đủ sau khi shop bấm thanh toán MANUAL — có {@code transactionId} trong nội dung CK + QR.
     */
    public SubscriptionTransferInstructionsDto buildForPayment(String transactionId,
                                                               String shopId,
                                                               String shopName,
                                                               long amountVnd) {
        if (!props.isDisplayReady()) {
            return null;
        }
        String content = expandTemplate(props.getTransferContentTemplate(), transactionId, shopId, shopName);
        return SubscriptionTransferInstructionsDto.builder()
                .bankName(trim(props.getBankName()))
                .accountNumber(trim(props.getAccountNumber()))
                .accountHolder(trim(props.getAccountHolder()))
                .amountVnd(amountVnd)
                .transferContent(content)
                .qrImageUrl(buildQrUrl(content, amountVnd))
                .instructions(buildPaymentInstructions(amountVnd, content))
                .build();
    }

    private String resolveStaticQrOnly() {
        if (notBlank(props.getStaticQrImageUrl())) {
            return props.getStaticQrImageUrl().trim();
        }
        return null;
    }

    private String buildQrUrl(String transferContent, long amountVnd) {
        if (notBlank(props.getStaticQrImageUrl())) {
            return props.getStaticQrImageUrl().trim();
        }
        if (!notBlank(props.getVietQrBankBin()) || !notBlank(props.getAccountNumber())) {
            return null;
        }
        String bin = props.getVietQrBankBin().trim();
        String acc = props.getAccountNumber().trim().replaceAll("\\s+", "");
        String base = "https://img.vietqr.io/image/" + bin + "-" + acc + "-compact2.jpg";
        String add = transferContent == null ? "" : trimForVietQrAddInfo(transferContent);
        String q = "amount=" + amountVnd;
        if (!add.isEmpty()) {
            q += "&addInfo=" + URLEncoder.encode(add, StandardCharsets.UTF_8);
        }
        return base + "?" + q;
    }

    /**
     * VietQR giới hạn độ dài addInfo — cắt an toàn để QR vẫn generate được.
     */
    private static String trimForVietQrAddInfo(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > 22 ? t.substring(0, 22) : t;
    }

    private String buildStaticInstructions(long amountVnd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Số tiền: ").append(String.format("%,d", amountVnd)).append(" ₫/tháng.\n");
        sb.append("Sau khi bấm \"Thanh toán\", hệ thống sẽ cấp mã giao dịch — vui lòng ghi đúng nội dung chuyển khoản.\n");
        sb.append("Admin sẽ xác nhận trong thời gian làm việc; bạn nhận thông báo khi gia hạn thành công.");
        return sb.toString();
    }

    private String buildPaymentInstructions(long amountVnd, String transferContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("1) Chuyển đúng ").append(String.format("%,d", amountVnd)).append(" ₫ tới tài khoản trên.\n");
        sb.append("2) Nội dung CK: ").append(transferContent).append(" (bắt buộc khớp).\n");
        sb.append("3) Sau khi chuyển, chờ admin xác nhận — bạn sẽ nhận thông báo khi thanh toán được ghi nhận.");
        return sb.toString();
    }

    private static String expandTemplate(String template, String transactionId, String shopId, String shopName) {
        String t = template == null ? "GD {transactionId}" : template;
        return t.replace("{transactionId}", nullToEmpty(transactionId))
                .replace("{shopId}", nullToEmpty(shopId))
                .replace("{shopName}", nullToEmpty(shopName));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
