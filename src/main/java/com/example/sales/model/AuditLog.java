// File: src/main/java/com/example/sales/model/AuditLog.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("audit_logs")
public class AuditLog extends BaseEntity {

    @Id
    private String id;

    private String userId;
    private String shopId;

    private String targetId;     // ID của Order hoặc Product
    private String targetType;   // "ORDER", "PRODUCT"

    private String action;       // PRICE_CHANGED, STATUS_UPDATED, etc.
    private String description;
}

