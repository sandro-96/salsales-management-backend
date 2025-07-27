// File: src/main/java/com/example/sales/constant/ShopType.java
package com.example.sales.constant;

import lombok.Getter;

@Getter
public enum ShopType {
    RESTAURANT(ShopIndustry.FNB, BusinessModel.DINE_IN, false),
    CAFE(ShopIndustry.FNB, BusinessModel.DINE_IN, false),
    BAR(ShopIndustry.FNB, BusinessModel.DINE_IN, false),

    GROCERY(ShopIndustry.RETAIL, BusinessModel.PHYSICAL_STORE, true),
    CONVENIENCE(ShopIndustry.RETAIL, BusinessModel.PHYSICAL_STORE, true),
    PHARMACY(ShopIndustry.HEALTHCARE, BusinessModel.PHYSICAL_STORE, true),
    RETAIL(ShopIndustry.RETAIL, BusinessModel.PHYSICAL_STORE, true),

    SALON(ShopIndustry.SERVICE, BusinessModel.APPOINTMENT_BASED, false),
    TUTORING_CENTER(ShopIndustry.EDUCATION, BusinessModel.APPOINTMENT_BASED, false),

    OTHER(ShopIndustry.OTHER, BusinessModel.PHYSICAL_STORE, false);

    private final ShopIndustry industry;
    private final BusinessModel businessModel;
    private final boolean trackInventory;

    ShopType(ShopIndustry industry, BusinessModel businessModel, boolean trackInventory) {
        this.industry = industry;
        this.businessModel = businessModel;
        this.trackInventory = trackInventory;
    }
}




