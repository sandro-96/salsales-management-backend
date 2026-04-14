package com.example.sales.util;

import com.example.sales.model.Order;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Mã đơn hàng hiển thị (lưu {@link Order#getOrderCode()} hoặc suy từ id đơn cũ).
 */
public final class OrderDisplayUtils {

    private OrderDisplayUtils() {
    }

    public static String displayOrderCode(Order order) {
        if (order != null && StringUtils.hasText(order.getOrderCode())) {
            return order.getOrderCode().trim();
        }
        return legacyOrderDisplayCode(order != null ? order.getId() : null);
    }

    public static String legacyOrderDisplayCode(String mongoId) {
        if (!StringUtils.hasText(mongoId)) {
            return "";
        }
        String t = mongoId.trim();
        if (t.length() <= 10) {
            return t.toUpperCase(Locale.ROOT);
        }
        return "DH-" + t.substring(t.length() - 8).toUpperCase(Locale.ROOT);
    }

    /**
     * Thay id đơn trong ghi chú kho bằng mã đơn hàng hiển thị (nếu tra được {@link Order}).
     */
    public static String enrichInventoryNote(String note, String referenceId, Order orderOrNull) {
        if (!StringUtils.hasText(note) || !StringUtils.hasText(referenceId) || orderOrNull == null) {
            return note;
        }
        if (!note.contains(referenceId)) {
            return note;
        }
        String code = displayOrderCode(orderOrNull);
        return note.replace(referenceId, code);
    }
}
