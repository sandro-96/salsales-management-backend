package com.example.sales.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserOrderHistoryItem {
    private String id;
    private String orderCode;
    private String shopId;
    private String shopName;
    private String shopSlug;
    private double totalAmount;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private int itemCount;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String customerNote;

    private List<UserOrderHistoryLineItem> items;
}
