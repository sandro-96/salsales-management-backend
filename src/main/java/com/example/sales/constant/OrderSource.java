// File: src/main/java/com/example/sales/constant/OrderSource.java
package com.example.sales.constant;

/**
 * Nguồn phát sinh đơn hàng.
 * - POS: tạo qua POS / quản trị nội bộ (mặc định cho dữ liệu cũ).
 * - ONLINE: tạo qua storefront công khai theo slug (guest checkout, COD).
 */
public enum OrderSource {
    POS,
    ONLINE
}
