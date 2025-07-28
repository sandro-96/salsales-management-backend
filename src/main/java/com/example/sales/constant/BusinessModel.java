package com.example.sales.constant;

/**
 * Mô hình kinh doanh / hình thức vận hành của shop.
 */
public enum BusinessModel {
    DINE_IN,            // Khách ăn/uống tại chỗ
    TAKEAWAY,           // Mang đi
    DELIVERY,           // Giao hàng
    ONLINE,             // Bán online (app, website)
    PHYSICAL_STORE,     // Cửa hàng truyền thống
    APPOINTMENT_BASED,  // Theo lịch hẹn (salon, spa, bác sĩ)
    HYBRID              // Kết hợp nhiều mô hình
}
