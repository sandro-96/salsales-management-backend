package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("table_groups")
public class TableGroup extends BaseEntity {
    @Id
    private String id;

    private String shopId;
    private String branchId;

    /** Optional label for UI */
    private String name;

    /** Tables in this group */
    private List<String> tableIds;
}

