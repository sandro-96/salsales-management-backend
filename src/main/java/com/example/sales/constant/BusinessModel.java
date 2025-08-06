package com.example.sales.constant;

import lombok.Getter;

@Getter
public enum BusinessModel {
    DINE_IN("Ăn tại chỗ"),
    TAKEAWAY("Mang đi"),
    DELIVERY("Giao hàng"),
    ONLINE("Bán online"),
    PHYSICAL_STORE("Cửa hàng"),
    APPOINTMENT_BASED("Theo lịch hẹn"),
    HYBRID("Kết hợp");

    private final String label;

    BusinessModel(String label) {
        this.label = label;
    }

}

