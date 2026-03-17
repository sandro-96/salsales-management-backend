package com.example.sales.util;

import org.springframework.util.StringUtils;

import java.text.Normalizer;

/**
 * Tiện ích chuẩn hóa tên danh mục sản phẩm.
 *
 * Hai cấp độ chuẩn hóa:
 *   - {@link #normalize}     : lưu vào DB — giữ dấu tiếng Việt, Title Case, 1 khoảng cách
 *   - {@link #toSkuSegment}  : dùng trong SKU prefix — bỏ dấu, thay space bằng '_', in hoa
 *
 * Ví dụ: "cá   tưỡi NGON" → normalize → "Cá Tưỡi Ngon"
 *                           → toSkuSegment → "CA_TUOI_NGON"
 */
public final class CategoryUtils {

    private CategoryUtils() {}

    /**
     * Chuẩn hóa category để lưu DB:
     * trim → collapse nhiều space thành 1 → Title Case mỗi từ.
     *
     * <pre>
     *   "cá  tươi NGON"  → "Cá Tươi Ngon"
     *   "  đồ UỐNG "     → "Đồ Uống"
     * </pre>
     */
    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) return raw;
        String[] words = raw.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Chuyển category thành segment dùng trong SKU prefix:
     * bỏ dấu tiếng Việt → thay space bằng '_' → in hoa → chỉ giữ [A-Z0-9_].
     *
     * <pre>
     *   "Cá Tươi Ngon"   → "CA_TUOI_NGON"
     *   "Đồ Uống"        → "DO_UONG"
     *   "Hóa Mỹ Phẩm"   → "HOA_MY_PHAM"
     * </pre>
     */
    public static String toSkuSegment(String category) {
        if (!StringUtils.hasText(category)) return "";
        // NFD decompose → xóa combining diacritics (dấu)
        String stripped = Normalizer.normalize(category, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // đ/Đ không được NFD decompose — xử lý thủ công
        stripped = stripped.replace("đ", "d").replace("Đ", "D");
        // Collapse spaces → '_', in hoa, loại ký tự đặc biệt
        return stripped.trim()
                .replaceAll("\\s+", "_")
                .toUpperCase()
                .replaceAll("[^A-Z0-9_]", "");
    }
}

