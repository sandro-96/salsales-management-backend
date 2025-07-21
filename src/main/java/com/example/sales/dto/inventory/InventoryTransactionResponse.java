// File: src/main/java/com/example/sales/dto/inventory/InventoryTransactionResponse.java
package com.example.sales.dto.inventory;

import com.example.sales.constant.InventoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // Bao gồm @Getter, @Setter, @EqualsAndHashCode, @ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {
    private String id;
    private String shopId;
    private String branchId;
    private String branchProductId; // ID của BranchProduct liên quan đến giao dịch
    private String productName;     // Tên của sản phẩm (từ Product master)
    private InventoryType type;     // Loại giao dịch (IMPORT, EXPORT, ADJUSTMENT)
    private int quantity;           // Số lượng thay đổi trong giao dịch
    private String note;
    private String referenceId;     // ID tham chiếu (ví dụ: Order ID)
    private LocalDateTime createdAt;
    private String createdBy;       // ID người dùng thực hiện giao dịch
}