// File: src/main/java/com/example/sales/model/InventoryTransaction.java
package com.example.sales.model;

import com.example.sales.constant.InventoryType;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("inventory_transactions")
public class InventoryTransaction extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String branchId;
    private String productId;

    /** null = giao dịch cấp BranchProduct; có giá trị = tồn theo biến thể */
    private String variantId;

    private InventoryType type;  // IMPORT, EXPORT, ADJUSTMENT

    private int quantity;        // Số lượng thay đổi (delta cho IMPORT/EXPORT, newQuantity cho ADJUSTMENT)

    private int currentStock;    // Tồn kho SAU giao dịch — snapshot để reconstruct timeline

    // Denormalized fields — snapshot tại thời điểm giao dịch, tránh N+1 khi load history
    private String productName;
    private String sku;

    private String note;

    private String referenceId;  // Liên kết đơn hàng, phiếu nhập, v.v.
}

