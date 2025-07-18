// File: src/main/java/com/example/sales/dto/order/OrderUpdateRequest.java
package com.example.sales.dto.order;
import lombok.Data;
import java.util.List;

@Data
public class OrderUpdateRequest {
    private String note;
    private String tableId;
    private List<OrderItemRequest> items;
}
