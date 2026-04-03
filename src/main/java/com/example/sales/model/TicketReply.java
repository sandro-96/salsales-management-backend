package com.example.sales.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketReply {
    private String id;
    private String userId;
    private String userName;
    private String message;
    private LocalDateTime createdAt;
}
