package com.example.sales.dto.tableGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableGroupRequest {
    @NotBlank(message = "shopId không được để trống")
    private String shopId;

    @NotBlank(message = "branchId không được để trống")
    private String branchId;

    private String name;

    @NotEmpty(message = "tableIds không được để trống")
    private List<String> tableIds;
}

