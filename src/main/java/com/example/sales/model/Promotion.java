// File: src/main/java/com/example/sales/model/Promotion.java
package com.example.sales.model;

import com.example.sales.constant.DiscountType;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "promotions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String branchId; // có thể null nếu áp dụng toàn bộ chi nhánh

    private String name;

    private DiscountType discountType; // PERCENT or AMOUNT

    private double discountValue;

    private List<String> applicableProductIds; // nếu null hoặc empty: áp dụng toàn bộ

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean active;
}
