package com.example.sales.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("next_sequences")
@CompoundIndex(def = "{'shopId': 1, 'prefix': 1, 'type': 1}", unique = true)
public class NextSequence {
    @Id
    private String id;
    private String shopId;
    private String prefix; // Ví dụ: FNB_FOOD, RETAIL_GROCERY
    private String type; // Ví dụ: SKU, ORDER, INVENTORY
    private int nextSequence; // Số thứ tự tiếp theo (1, 2, 3, ...)
}