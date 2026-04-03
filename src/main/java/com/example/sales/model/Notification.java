package com.example.sales.model;

import com.example.sales.constant.NotificationType;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@CompoundIndex(name = "user_read_idx", def = "{'userId': 1, 'read': 1}")
@CompoundIndex(name = "user_shop_idx", def = "{'userId': 1, 'shopId': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String userId;

    private NotificationType type;
    private String title;
    private String message;

    @Builder.Default
    private boolean read = false;

    private String referenceId;
    private String referenceType;

    private String actorId;
    private String actorName;
}
