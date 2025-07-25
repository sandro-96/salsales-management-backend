// File: src/main/java/com/example/sales/model/Customer.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("customers")
public class Customer extends BaseEntity {

    @Id
    private String id;

    private String userId;     // Chủ sở hữu
    private String name;
    private String phone;
    private String email;
    private String address;
    private String note;
    private String shopId;
    private String branchId;   // Có thể null nếu không phân biệt chi nhánh
}

