package com.example.sales.model;

import com.example.sales.constant.TicketCategory;
import com.example.sales.constant.TicketPriority;
import com.example.sales.constant.TicketStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "support_tickets")
@CompoundIndex(name = "shop_status_idx", def = "{'shopId': 1, 'status': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SupportTicket extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String userId;
    private String userEmail;
    private String userName;

    private String subject;
    private String message;

    private TicketCategory category;
    private TicketPriority priority;

    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    private String assigneeId;
    private String assigneeName;

    @Builder.Default
    private List<TicketReply> replies = new ArrayList<>();
}
