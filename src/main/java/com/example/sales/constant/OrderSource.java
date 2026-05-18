// File: src/main/java/com/example/sales/constant/OrderSource.java
package com.example.sales.constant;

import java.util.List;

/**
 * Nguồn phát sinh đơn hàng.
 * - POS: tạo qua POS / quản trị nội bộ (mặc định cho dữ liệu cũ).
 * - ONLINE: tạo qua storefront công khai theo slug (guest checkout, COD giao hàng).
 * - IN_STORE: guest tự order tại bàn qua QR code (dine-in self-service); staff thu tiền tại quầy.
 */
public enum OrderSource {
    POS,
    ONLINE,
    IN_STORE;

    /** Đơn khách tự đặt (storefront + QR bàn) — hiển thị chung tab Online. */
    public static List<OrderSource> guestCheckoutSources() {
        return List.of(ONLINE, IN_STORE);
    }

    public boolean isGuestCheckout() {
        return this == ONLINE || this == IN_STORE;
    }
}
