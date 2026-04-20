// File: src/main/java/com/example/sales/service/auth/TotpCodec.java
package com.example.sales.service.auth;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP (RFC 6238) minimal implementation với HMAC-SHA1, 30s step, 6 số. Base32
 * encode/decode theo RFC 4648 để tương thích Google Authenticator.
 */
public final class TotpCodec {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int SECRET_BYTES = 20;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private TotpCodec() {}

    public static String randomBase32Secret() {
        byte[] buf = new byte[SECRET_BYTES];
        RNG.nextBytes(buf);
        return encodeBase32(buf);
    }

    public static String otpAuthUri(String issuer, String account, String secret) {
        String label = urlEncode(issuer + ":" + account);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    /** Verify with ±1 step tolerance for clock drift. */
    public static boolean verify(String base32Secret, String code) {
        if (base32Secret == null || code == null) return false;
        String normalized = code.replace(" ", "").trim();
        if (!normalized.matches("\\d{" + DIGITS + "}")) return false;
        byte[] key = decodeBase32(base32Secret);
        long counter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int offset = -1; offset <= 1; offset++) {
            String generated = generate(key, counter + offset);
            if (constantTimeEquals(generated, normalized)) return true;
        }
        return false;
    }

    public static String generate(byte[] key, long counter) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % pow10(DIGITS);
            String code = Integer.toString(otp);
            while (code.length() < DIGITS) code = "0" + code;
            return code;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot generate TOTP", ex);
        }
    }

    private static int pow10(int n) {
        int r = 1;
        for (int i = 0; i < n; i++) r *= 10;
        return r;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    public static String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                int idx = (buffer >> (bits - 5)) & 0x1f;
                sb.append(BASE32.charAt(idx));
                bits -= 5;
            }
        }
        if (bits > 0) {
            int idx = (buffer << (5 - bits)) & 0x1f;
            sb.append(BASE32.charAt(idx));
        }
        return sb.toString();
    }

    public static byte[] decodeBase32(String input) {
        String s = input.replace(" ", "").toUpperCase().replaceAll("=+$", "");
        int buffer = 0, bits = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            int idx = BASE32.indexOf(s.charAt(i));
            if (idx < 0) throw new IllegalArgumentException("Invalid Base32");
            buffer = (buffer << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
