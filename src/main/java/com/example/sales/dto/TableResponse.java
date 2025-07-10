package com.example.sales.dto;

import com.example.sales.constant.TableStatus;
import lombok.*;

@Getter
@Setter
@Builder
public class TableResponse {
    private String id;
    private String name;
    private TableStatus status;
    private String shopId;
    private String shopName;
    private Integer capacity;
    private String note;
    private String currentOrderId;
}


