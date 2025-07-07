package com.example.sales.model;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Document("orders")
@Data
public class Order extends BaseEntity {
    @Id
    private String id;

    private String userId; // Người đặt đơn
    private List<OrderItem> items;
    private double totalPrice;
    private OrderStatus status = OrderStatus.PENDING;
}
