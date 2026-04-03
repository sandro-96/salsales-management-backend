package com.example.sales.dto.customer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String note;
    private String branchId;
    private long loyaltyPoints;
    private long totalPointsEarned;
    private long totalPointsRedeemed;
    private LocalDateTime createdAt;
}
