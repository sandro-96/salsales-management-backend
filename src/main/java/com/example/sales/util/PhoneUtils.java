package com.example.sales.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Chuẩn hoá SĐT để so khớp đơn storefront với tài khoản user.
 */
public final class PhoneUtils {

    private PhoneUtils() {
    }

    /** Bỏ khoảng trắng, giữ ký tự số và + ở đầu. */
    public static String compact(String phone) {
        if (phone == null) {
            return null;
        }
        String t = phone.trim().replaceAll("\\s+", "");
        return t.isEmpty() ? null : t;
    }

    /**
     * Dạng chuẩn để lưu/so khớp: SĐT VN 10 chữ số bắt đầu bằng 0; các quốc gia khác chỉ giữ chữ số.
     */
    public static String normalizeForMatch(String phone) {
        String compact = compact(phone);
        if (compact == null) {
            return null;
        }
        String digits = compact.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("84") && digits.length() >= 11) {
            digits = "0" + digits.substring(2);
        } else if (digits.length() == 9 && !digits.startsWith("0")) {
            digits = "0" + digits;
        }
        return digits;
    }

    /** Các biến thể guestPhone có thể đã lưu trước khi có guestPhoneNormalized. */
    public static List<String> matchVariants(String normalized) {
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        Set<String> variants = new LinkedHashSet<>();
        variants.add(normalized);
        if (normalized.startsWith("0") && normalized.length() == 10) {
            String withoutZero = normalized.substring(1);
            variants.add("84" + withoutZero);
            variants.add("+84" + withoutZero);
            variants.add("0" + withoutZero);
        }
        return new ArrayList<>(variants);
    }
}
