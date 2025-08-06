package com.example.sales.dto;

import com.example.sales.constant.ShopType;

public record ShopTypeOption(String value, String label, boolean trackInventory, String defaultBusinessModel) {
    public static ShopTypeOption from(ShopType type) {
        return new ShopTypeOption(type.name(), type.getLabel(), type.isTrackInventory(), type.getDefaultBusinessModel().name());
    }
}
