package com.example.sales.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistory {
    private double price; // Giá bán
    private double costPrice; // Giá nhập
    private LocalDateTime effectiveDate; // Ngày áp dụng
}
