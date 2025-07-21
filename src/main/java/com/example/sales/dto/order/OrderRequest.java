// File: src/main/java/com/example/sales/dto/order/OrderRequest.java
package com.example.sales.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotNull
    @Valid
    private List<OrderItemRequest> items;

    private String tableId; // optional
    private String note;
    private String branchId;
}
