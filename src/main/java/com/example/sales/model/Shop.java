// File: src/main/java/com/example/sales/model/Shop.java
package com.example.sales.model;

import com.example.sales.constant.ShopType;
import com.example.sales.model.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "shops")
public class Shop extends BaseEntity {

    @Id
    private String id;

    private String name;
    private String ownerId; // user.id
    private ShopType type; // RESTAURANT, GROCERY, CONVENIENCE, ...
    private String address;
    private String phone;
    private String logoUrl; // nếu cần logo
    private boolean active = true;

    // ====== Nâng cao (SS) ======
    private boolean trackInventory = true; // Có quản lý tồn kho không
    private String currency = "VND";       // Đơn vị tiền tệ mặc định
    private String timezone = "Asia/Ho_Chi_Minh"; // Múi giờ shop
    private String orderPrefix = "ORD";    // Tiền tố mã đơn hàng
}
