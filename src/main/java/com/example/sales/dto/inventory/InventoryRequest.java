// File: src/main/java/com/example/sales/dto/inventory/InventoryRequest.java
package com.example.sales.dto.inventory;

import com.example.sales.constant.InventoryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotBlank
    private String productId;

    private String branchId; // optional

    @NotNull
    private InventoryType type;

    @Min(1)
    private int quantity;

    private String note;
}
