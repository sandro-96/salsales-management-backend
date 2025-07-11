package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("branches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch extends BaseEntity {

    @Id
    private String id;

    private String shopId;

    private String name;
    private String address;
    private String phone;

    @Builder.Default
    private boolean active = true;
}
