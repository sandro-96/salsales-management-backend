package com.example.sales.dto;

import com.example.sales.constant.TableStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableRequest {
    @NotBlank
    private String name;

    private TableStatus status;

    private String shopId;
}
