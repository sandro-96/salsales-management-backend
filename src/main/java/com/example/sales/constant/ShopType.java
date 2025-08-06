package com.example.sales.constant;

import lombok.Getter;

@Getter
public enum ShopType {
    RESTAURANT(ShopIndustry.FNB, false, "Nhà hàng", BusinessModel.DINE_IN),
    CAFE(ShopIndustry.FNB, false, "Quán cafe", BusinessModel.DINE_IN),
    BAR(ShopIndustry.FNB, false, "Quán bar", BusinessModel.DINE_IN),

    GROCERY(ShopIndustry.RETAIL, true, "Tạp hóa", BusinessModel.PHYSICAL_STORE),
    CONVENIENCE(ShopIndustry.RETAIL, true, "Cửa hàng tiện lợi", BusinessModel.PHYSICAL_STORE),
    PHARMACY(ShopIndustry.HEALTHCARE, true, "Hiệu thuốc", BusinessModel.PHYSICAL_STORE),
    RETAIL(ShopIndustry.RETAIL, true, "Cửa hàng bán lẻ", BusinessModel.PHYSICAL_STORE),

    SALON(ShopIndustry.SERVICE, false, "Salon", BusinessModel.APPOINTMENT_BASED),
    TUTORING_CENTER(ShopIndustry.EDUCATION, false, "Trung tâm dạy học", BusinessModel.APPOINTMENT_BASED),

    OTHER(ShopIndustry.OTHER, false, "Khác", BusinessModel.HYBRID);

    private final ShopIndustry industry;
    private final boolean trackInventory;
    private final String label;
    private final BusinessModel defaultBusinessModel; // 👈 Thêm field

    ShopType(ShopIndustry industry, boolean trackInventory, String label, BusinessModel defaultBusinessModel) {
        this.industry = industry;
        this.trackInventory = trackInventory;
        this.label = label;
        this.defaultBusinessModel = defaultBusinessModel;
    }
}
