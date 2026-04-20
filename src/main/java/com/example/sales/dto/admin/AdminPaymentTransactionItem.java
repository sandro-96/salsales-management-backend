package com.example.sales.dto.admin;

import com.example.sales.constant.PaymentGatewayType;
import com.example.sales.constant.PaymentTransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPaymentTransactionItem {
    private String id;
    private String shopId;
    private String shopName;
    private String ownerId;
    private PaymentGatewayType gateway;
    private String providerTxnRef;
    private String providerTransNo;
    private long amountVnd;
    private PaymentTransactionStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
