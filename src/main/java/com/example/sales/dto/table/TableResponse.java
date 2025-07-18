// File: src/main/java/com/example/sales/dto/table/TableResponse.java
package com.example.sales.dto.table;

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
    private String branchId; // Có thể null nếu không phân biệt chi nhánh
}


