package com.example.sales.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hướng dẫn chuyển khoản cho shop (MANUAL) — trả về từ {@code /subscription/pay}
 * hoặc {@code /subscription/transfer-info}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTransferInstructionsDto {

    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private long amountVnd;

    /** Nội dung CK shop phải ghi đúng để admin đối soát. */
    private String transferContent;

    /** URL ảnh QR (VietQR hoặc static) — có thể null nếu chưa cấu hình QR. */
    private String qrImageUrl;

    /** Gợi ý hiển thị (đa dòng, plain text). */
    private String instructions;
}
