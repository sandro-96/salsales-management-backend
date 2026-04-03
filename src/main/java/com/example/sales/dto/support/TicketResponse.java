package com.example.sales.dto.support;

import com.example.sales.constant.TicketCategory;
import com.example.sales.constant.TicketPriority;
import com.example.sales.constant.TicketStatus;
import com.example.sales.model.TicketReply;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TicketResponse {
    private String id;
    private String shopId;
    private String userId;
    private String userEmail;
    private String userName;

    private String subject;
    private String message;

    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;

    private String assigneeId;
    private String assigneeName;

    private List<TicketReply> replies;
    private int replyCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
