package com.example.sales.constant;

import lombok.Getter;

@Getter
public enum ShopType {
    RESTAURANT(ShopIndustry.FNB, "Nhà hàng", BusinessModel.DINE_IN),
    CAFE(ShopIndustry.FNB, "Quán cafe", BusinessModel.DINE_IN),
    BAR(ShopIndustry.FNB, "Quán bar", BusinessModel.DINE_IN),
    GROCERY(ShopIndustry.RETAIL, "Tạp hóa", BusinessModel.PHYSICAL_STORE),
    CONVENIENCE(ShopIndustry.RETAIL, "Cửa hàng tiện lợi", BusinessModel.PHYSICAL_STORE),
    PHARMACY(ShopIndustry.HEALTHCARE, "Hiệu thuốc", BusinessModel.PHYSICAL_STORE),
    RETAIL(ShopIndustry.RETAIL, "Cửa hàng bán lẻ", BusinessModel.PHYSICAL_STORE),
    SALON(ShopIndustry.SERVICE, "Salon", BusinessModel.APPOINTMENT_BASED),
    TUTORING_CENTER(ShopIndustry.EDUCATION, "Trung tâm dạy học", BusinessModel.APPOINTMENT_BASED),
    OTHER(ShopIndustry.OTHER, "Khác", BusinessModel.HYBRID);
    private final ShopIndustry industry;
    private final String label;
    private final BusinessModel defaultBusinessModel;

    ShopType(ShopIndustry industry, String label, BusinessModel defaultBusinessModel) {
        this.industry = industry;
        this.label = label;
        this.defaultBusinessModel = defaultBusinessModel;
    }
}
