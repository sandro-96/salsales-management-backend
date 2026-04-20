// File: src/main/java/com/example/sales/util/WeightUnitConverter.java
package com.example.sales.util;

import org.springframework.util.StringUtils;

/**
 * Quy đổi {@code weight} nhập từ POS (đơn vị tự nhiên, ví dụ kg/g/l/ml)
 * về đơn vị cơ sở nguyên (base unit) để lưu tồn trong {@code BranchProduct.stockInBaseUnits}.
 * <ul>
 *   <li>kg → gram (×1000)</li>
 *   <li>g  → gram (×1)</li>
 *   <li>l  → ml (×1000)</li>
 *   <li>ml → ml (×1)</li>
 *   <li>khác → 1-1 (không chuyển đổi)</li>
 * </ul>
 * Kết quả làm tròn nửa-lên để tránh chênh lệch tích luỹ nhiều lần bán.
 */
public final class WeightUnitConverter {

    private WeightUnitConverter() {}

    public static long toBaseUnits(double weight, String unit) {
        double multiplier = multiplierFor(unit);
        double raw = weight * multiplier;
        if (raw < 0) return 0L;
        return Math.round(raw);
    }

    public static String baseUnitLabel(String unit) {
        String u = normalize(unit);
        return switch (u) {
            case "kg", "g" -> "g";
            case "l", "ml" -> "ml";
            default -> StringUtils.hasText(unit) ? unit : "unit";
        };
    }

    public static double fromBaseUnits(long baseUnits, String unit) {
        double multiplier = multiplierFor(unit);
        if (multiplier == 0) return baseUnits;
        return baseUnits / multiplier;
    }

    private static double multiplierFor(String unit) {
        return switch (normalize(unit)) {
            case "kg", "l" -> 1000.0;
            case "g", "ml" -> 1.0;
            default -> 1.0;
        };
    }

    private static String normalize(String unit) {
        return StringUtils.hasText(unit) ? unit.trim().toLowerCase() : "";
    }
}
