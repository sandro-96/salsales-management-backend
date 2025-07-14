// File: src/main/java/com/example/sales/model/AuditLog.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("audit_logs")
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuditLog extends BaseEntity {

    @Id
    private String id;

    private String userId;
    private String shopId;

    private String targetId;     // ID của Order hoặc Product
    private String targetType;   // "ORDER", "PRODUCT"

    private String action;       // Hành động: PRICE_CHANGED, STATUS_UPDATED...
    private String description;  // Mô tả cụ thể
}
