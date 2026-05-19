package com.example.sales.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** Chuẩn hóa danh sách SĐT (giữ {@code phone} là SĐT chính — phần tử đầu). */
public final class PhoneContactUtils {

    private PhoneContactUtils() {
    }

    public static List<String> normalizePhones(List<String> phones, String legacyPhone) {
        List<String> out = new ArrayList<>();
        if (phones != null) {
            for (String raw : phones) {
                if (!StringUtils.hasText(raw)) {
                    continue;
                }
                String trimmed = raw.trim();
                if (!out.contains(trimmed)) {
                    out.add(trimmed);
                }
            }
        }
        if (out.isEmpty() && StringUtils.hasText(legacyPhone)) {
            out.add(legacyPhone.trim());
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    public static String primaryPhone(List<String> phones) {
        return phones == null || phones.isEmpty() ? null : phones.get(0);
    }

    public static List<String> resolveForResponse(List<String> phones, String legacyPhone) {
        return normalizePhones(phones, legacyPhone);
    }
}
