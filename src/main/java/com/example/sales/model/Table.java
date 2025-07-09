package com.example.sales.model;

import com.example.sales.constant.TableStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
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

    private String name;

    private TableStatus status;

    @DBRef
    private Shop shop;
}
