// File: src/main/java/com/example/sales/dto/table/TableRequest.java
package com.example.sales.dto.table;

import com.example.sales.constant.TableStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableRequest {
    @NotBlank
    private String name;

    private TableStatus status = TableStatus.AVAILABLE;
    private String shopId;
    private Integer capacity;
    private String note;
    private String branchId; // Có thể null nếu không phân biệt chi nhánh
}

