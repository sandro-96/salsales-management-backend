// File: src/main/java/com/example/sales/model/Shop.java
package com.example.sales.model;

import com.example.sales.constant.ShopType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "shops")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class Shop extends BaseEntity {

    @Id
    private String id;

    private String name;
    private String ownerId; // user.id
    private ShopType type; // RESTAURANT, GROCERY, CONVENIENCE, ...
    private String address;
    private String phone;
    private String logoUrl; // nếu cần logo
    @Builder.Default
    private boolean active = true;

    // ====== Nâng cao (SS) ======
    @Builder.Default
    private boolean trackInventory = true; // Có quản lý tồn kho không
    @Builder.Default
    private String currency = "VND";       // Đơn vị tiền tệ mặc định
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh"; // Múi giờ shop
    @Builder.Default
    private String orderPrefix = "ORD";    // Tiền tố mã đơn hàng
    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.FREE;
    private LocalDateTime planExpiry; // optional nếu bạn muốn gói hết hạn
}
