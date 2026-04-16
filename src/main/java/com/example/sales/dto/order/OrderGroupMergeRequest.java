package com.example.sales.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderGroupMergeRequest {
    /** Đơn đích (sẽ giữ lại để thanh toán). */
    @NotBlank
    private String targetOrderId;

    /** Danh sách đơn nguồn cần gộp vào đơn đích (có thể bao gồm cả đơn đích — sẽ được loại trùng ở service). */
    @NotEmpty
    private List<@NotBlank String> sourceOrderIds;
}
