package com.example.sales.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionResponse {
    private String id;
    private String type;
    private long points;
    private long balanceAfter;
    private String referenceId;
    private String note;
    private LocalDateTime createdAt;
}
