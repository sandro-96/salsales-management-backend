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
}
