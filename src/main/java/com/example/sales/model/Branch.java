// File: src/main/java/com/example/sales/model/Branch.java
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
@Document("branches")
public class Branch extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String name;
    private String address;
    private String phone;

    @Builder.Default
    private boolean active = true;
}

