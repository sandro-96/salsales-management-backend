// File: src/main/java/com/example/sales/model/SubscriptionHistory.java
package com.example.sales.model;

import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionPlan;
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
@Document("subscription_histories")
public class SubscriptionHistory extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String userId;

    private SubscriptionPlan oldPlan;
    private SubscriptionPlan newPlan;
    private int durationMonths;

    private String transactionId;
    private String paymentMethod;
    private SubscriptionActionType actionType;
}

