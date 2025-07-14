// File: ShopType.java

package com.example.sales.constant;

import lombok.Getter;

@Getter
public enum ShopType {
    RESTAURANT(false),
    CAFE(false),
    BAR(false),
    GROCERY(true),
    CONVENIENCE(true),
    PHARMACY(true),
    RETAIL(true),
    OTHER(false);

    private final boolean trackInventory;

    ShopType(boolean trackInventory) {
        this.trackInventory = trackInventory;
    }

}


