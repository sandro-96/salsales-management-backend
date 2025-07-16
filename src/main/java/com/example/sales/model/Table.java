// File: src/main/java/com/example/sales/model/Table.java
package com.example.sales.model;

import com.example.sales.constant.TableStatus;
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
@Document(collection = "tables")
public class Table extends BaseEntity {
    @Id
    private String id;

    private String shopId;
    private String branchId;
    private String name;
    private TableStatus status;

    private Integer capacity;
    private String note;
    private String currentOrderId;
}

