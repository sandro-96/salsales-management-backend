package com.example.sales.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Ghi lại một lần thay đổi giá bán hoặc giá nhập.
 * Được tự động append bởi service — client không được ghi trực tiếp.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistory {
    private double oldPrice;      // Giá bán trước khi thay đổi
    private double newPrice;      // Giá bán sau khi thay đổi
    private double oldCostPrice;  // Giá nhập trước khi thay đổi
    private double newCostPrice;  // Giá nhập sau khi thay đổi
    private LocalDateTime changedAt;  // Thời điểm thay đổi (tự set bởi server)
    private String changedBy;     // userId của người thực hiện thay đổi
    private String reason;        // Lý do thay đổi (tùy chọn, do client truyền lên)
}
