// File: src/main/java/com/example/sales/model/Order.java
package com.example.sales.model;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "items") // 👈 Quan trọng: không in danh sách item
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("orders")
public class Order extends BaseEntity {
    @Id
    private String id;

    private String shopId;
    private String branchId;
    private String tableId;
    private String userId;

    private List<OrderItem> items;

    private double totalPrice;
    private double totalAmount;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private String paymentId;
    private String paymentMethod;
    private LocalDateTime paymentTime;
    private boolean isPaid;

    private String note;
}
