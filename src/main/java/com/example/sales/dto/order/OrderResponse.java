// File: src/main/java/com/example/sales/dto/order/OrderResponse.java

package com.example.sales.dto.order;

import com.example.sales.constant.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private String id;
    private String tableId;
    private String note;
    private OrderStatus status;

    private boolean paid;
    private String paymentMethod;
    private String paymentId;
    private LocalDateTime paymentTime;

    private double totalAmount;
    private double totalPrice;

    private List<OrderItemResponse> items;
}
