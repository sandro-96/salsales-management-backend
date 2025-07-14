// File: src/main/java/com/example/sales/model/Order.java
package com.example.sales.model;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document("orders")
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order extends BaseEntity {
    @Id
    private String id;

    private String shopId;             // ✔️ Ràng buộc chặt với 1 shop
    private String branchId;            // ✔️ Ràng buộc chặt với 1 chi nhánh
    private String tableId;            // ✔️ Gắn bàn (có thể null)

    private String userId;             // Ai tạo đơn

    private List<OrderItem> items;     // Danh sách sản phẩm
    private double totalPrice;
    private double totalAmount;

    private OrderStatus status = OrderStatus.PENDING;

    // Thông tin thanh toán
    private String paymentId;
    private String paymentMethod;      // Momo, VNPay, Cash
    private LocalDateTime paymentTime;
    private boolean isPaid;

    private String note;
}