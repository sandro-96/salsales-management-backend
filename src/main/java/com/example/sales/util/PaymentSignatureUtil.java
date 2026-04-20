package com.example.sales.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Hash / signature helpers dùng chung cho VNPay (HMAC-SHA512 hex)
 * và MoMo (HMAC-SHA256 hex). Tách riêng khỏi {@link SignatureUtil}
 * để không đụng API webhook nội bộ (base64) hiện có.
 */
public final class PaymentSignatureUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PaymentSignatureUtil() {}

    public static String hmacSha512Hex(String data, String secret) {
        return hmacHex("HmacSHA512", data, secret);
    }

    public static String hmacSha256Hex(String data, String secret) {
        return hmacHex("HmacSHA256", data, secret);
    }

    private static String hmacHex(String algorithm, String data, String secret) {
        if (secret == null) throw new IllegalArgumentException("secret is null");
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] raw = mac.doFinal(data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute " + algorithm + ": " + ex.getMessage(), ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
        }
        return sb.toString();
    }
}
