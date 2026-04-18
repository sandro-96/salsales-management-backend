package com.example.sales.util;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chuẩn hoá và kiểm tra mã vạch GTIN (EAN-8/12/13/14, UPC-A).
 * Giảm lỗi nhập tay / mã không khớp máy quét so với catalog chuẩn (admin).
 */
public final class GtinBarcodeValidator {

    private GtinBarcodeValidator() {
    }

    public static String normalizeDigits(String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        String s = sb.toString();
        return s.isEmpty() ? null : s;
    }

    /**
     * Kiểm tra chữ số kiểm tra GS1 (modulo 10, trọng số 3,1 từ phải sang).
     */
    public static boolean verifyGs1CheckDigit(String fullWithCheck) {
        if (fullWithCheck == null || fullWithCheck.length() < 2) {
            return false;
        }
        for (int i = 0; i < fullWithCheck.length(); i++) {
            if (!Character.isDigit(fullWithCheck.charAt(i))) {
                return false;
            }
        }
        String body = fullWithCheck.substring(0, fullWithCheck.length() - 1);
        int expected = computeGs1CheckDigit(body);
        int actual = fullWithCheck.charAt(fullWithCheck.length() - 1) - '0';
        return expected == actual;
    }

    private static int computeGs1CheckDigit(String dataDigitsOnly) {
        int sum = 0;
        int len = dataDigitsOnly.length();
        for (int i = len - 1; i >= 0; i--) {
            int d = dataDigitsOnly.charAt(i) - '0';
            int posFromRight = len - 1 - i;
            int weight = (posFromRight % 2 == 0) ? 3 : 1;
            sum += d * weight;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Chuẩn hoá để lưu: chỉ chữ số; với UPC-A (12 số) hợp lệ → thêm 0 đầu thành EAN-13.
     * Ném {@link BusinessException} nếu độ dài chuẩn GS1 nhưng sai checksum.
     */
    public static String resolveForProductSave(String raw) {
        String n = normalizeDigits(raw);
        if (!StringUtils.hasText(n)) {
            return null;
        }
        int len = n.length();
        if (len == 8 || len == 12 || len == 13 || len == 14) {
            if (!verifyGs1CheckDigit(n)) {
                throw new BusinessException(ApiCode.BARCODE_INVALID_GSIN);
            }
            if (len == 12) {
                return "0" + n;
            }
        }
        return n;
    }

    /**
     * Các biến thể cần thử khi tra cứu catalog (quét EAN-13 vs lưu UPC-12 trong DB).
     */
    public static List<String> catalogLookupCandidates(String raw) {
        String n = normalizeDigits(raw);
        if (!StringUtils.hasText(n)) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        seen.add(n);
        if (n.length() == 13 && n.startsWith("0")) {
            seen.add(n.substring(1));
        }
        if (n.length() == 12) {
            seen.add("0" + n);
        }
        return new ArrayList<>(seen);
    }

    /**
     * Chỉ đồng bộ catalog chung khi mã đúng chuẩn GS1 (tránh ghi đè catalog bằng mã sai checksum).
     */
    public static boolean shouldPublishToSharedCatalog(String storedBarcode) {
        String n = normalizeDigits(storedBarcode);
        if (!StringUtils.hasText(n)) {
            return false;
        }
        int len = n.length();
        if (len == 8 || len == 12 || len == 13 || len == 14) {
            return verifyGs1CheckDigit(n);
        }
        return true;
    }
}
