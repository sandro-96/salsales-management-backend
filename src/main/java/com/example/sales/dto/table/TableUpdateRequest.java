// File: src/main/java/com/example/sales/dto/table/TableUpdateRequest.java
package com.example.sales.dto.table;

import com.example.sales.constant.TableStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TableUpdateRequest {
    @NotBlank
    private String name;
    private TableStatus status;
    private String shopId;
    @Min(1)
    private Integer capacity;
    private String note;
    private String branchId;
}
