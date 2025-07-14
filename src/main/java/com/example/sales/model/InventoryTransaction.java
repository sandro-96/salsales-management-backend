// File: src/main/java/com/example/sales/model/InventoryTransaction.java

package com.example.sales.model;

import com.example.sales.constant.InventoryType;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventory_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryTransaction extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String branchId;      // Có thể null nếu không phân biệt chi nhánh
    private String productId;

    private InventoryType type;   // IMPORT, EXPORT, ADJUSTMENT

    private int quantity;         // Dương nếu nhập, âm nếu xuất

    private String note;          // Ghi chú lý do nhập, xuất, điều chỉnh

    private String referenceId;   // ID liên quan (vd: orderId nếu export theo đơn)
}
