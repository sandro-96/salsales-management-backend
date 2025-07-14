// File: src/main/java/com/example/sales/model/ShopUser.java
package com.example.sales.model;

import com.example.sales.constant.ShopRole;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shop_users")
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopUser extends BaseEntity {

    @Id
    private String id;

    private String shopId;

    private String userId;

    private ShopRole role;
}
