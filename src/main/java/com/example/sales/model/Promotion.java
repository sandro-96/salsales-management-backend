// File: src/main/java/com/example/sales/model/Promotion.java
package com.example.sales.model;

import com.example.sales.constant.DiscountType;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "promotions")
public class Promotion extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String branchId;

    private String name;

    private DiscountType discountType; // PERCENT, AMOUNT

    private double discountValue;

    private List<String> applicableProductIds;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean active;
}
