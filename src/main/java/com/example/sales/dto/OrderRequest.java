package com.example.sales.dto;

import com.example.sales.model.OrderItem;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private List<OrderItem> items;
    private String tableId;      // ✅ có thể null
    private String note;
}
