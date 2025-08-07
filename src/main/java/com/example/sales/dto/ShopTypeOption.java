package com.example.sales.dto;

import com.example.sales.constant.ShopIndustry;
import com.example.sales.constant.ShopType;

public record ShopTypeOption(String value, String label, ShopIndustry industry, boolean trackInventory, String defaultBusinessModel) {
    public static ShopTypeOption from(ShopType type) {
        return new ShopTypeOption(type.name(), type.getLabel(), type.getIndustry(), type.isTrackInventory(), type.getDefaultBusinessModel().name());
    }
}
