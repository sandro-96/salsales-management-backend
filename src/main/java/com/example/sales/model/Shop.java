// File: src/main/java/com/example/sales/model/Shop.java
package com.example.sales.model;

import com.example.sales.constant.ShopType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "shops")
public class Shop extends BaseEntity {

    @Id
    private String id;

    private String name;
    private String ownerId;
    private ShopType type;
    private String address;
    private String phone;
    private String logoUrl;
    private String countryCode;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private String currency = "VND";

    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @Builder.Default
    private String orderPrefix = "ORD";

    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    private LocalDateTime planExpiry;

    public boolean isTrackInventory() {
        return type != null && type.isTrackInventory();
    }
}

