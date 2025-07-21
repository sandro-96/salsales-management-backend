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

    private InventoryType type;  // IMPORT, EXPORT, ADJUSTMENT

    private int quantity;

    private String note;

    private String referenceId;  // Liên kết đơn hàng, phiếu nhập, v.v.
}

