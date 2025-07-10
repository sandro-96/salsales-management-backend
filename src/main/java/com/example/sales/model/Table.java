// File: Table.java
package com.example.sales.model;

import com.example.sales.constant.TableStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Table extends BaseEntity {
    @Id
    private String id;

    private String shopId;               // ✔️ ID của cửa hàng
    private String name;                 // Tên bàn (B1, B2...)
    private TableStatus status;         // Trạng thái bàn

    private Integer capacity;           // Sức chứa (optional)
    private String note;                // Ghi chú (optional)
    private String currentOrderId;      // Đơn đang sử dụng bàn (nếu có)
}
