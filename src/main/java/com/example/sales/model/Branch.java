// File: src/main/java/com/example/sales/model/Branch.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

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
    private LocalDate openingDate;

    private LocalTime openingTime;
    private LocalTime closingTime;

    private String managerName;
    private String managerPhone;

    private Integer capacity;

    private String description;
    @Builder.Default
    private boolean isDefault = false;
    @Builder.Default
    private boolean active = true;
}

