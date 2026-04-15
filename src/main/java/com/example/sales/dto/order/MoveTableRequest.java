package com.example.sales.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveTableRequest {
    @NotBlank(message = "toTableId không được để trống")
    private String toTableId;
}

