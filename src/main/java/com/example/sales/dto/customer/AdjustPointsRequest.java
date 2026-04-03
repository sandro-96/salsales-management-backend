package com.example.sales.dto.customer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdjustPointsRequest {

    @NotNull(message = "Số điểm không được để trống")
    private Long points;

    private String note;
}
