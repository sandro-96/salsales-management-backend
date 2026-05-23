package com.example.sales.util;

import java.nio.charset.StandardCharsets;

/**
 * Sửa chuỗi tiếng Việt bị lỗi khi file .env / properties đọc UTF-8 nhầm thành ISO-8859-1 (Windows).
 */
public final class Utf8TextUtil {

    private Utf8TextUtil() {
    }

    /**
     * @return {@code input} đã sửa nếu có dấu hiệu mojibake; ngược lại giữ nguyên.
     */
    public static String fixUtf8Mojibake(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        if (!looksLikeMojibake(input)) {
            return input;
        }
        try {
            String fixed = new String(input.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            if (!fixed.contains("\uFFFD") && !fixed.isEmpty()) {
                return fixed;
            }
        } catch (Exception ignored) {
            // keep original
        }
        return input;
    }

    private static boolean looksLikeMojibake(String s) {
        return s.contains("Ã")
                || s.contains("á»")
                || s.contains("Æ°")
                || s.contains("Ä")
                || s.contains("áº");
    }
}
