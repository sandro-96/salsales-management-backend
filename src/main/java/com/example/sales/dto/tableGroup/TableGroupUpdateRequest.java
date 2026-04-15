package com.example.sales.dto.tableGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableGroupUpdateRequest {
    private String name;
    private List<String> tableIds;
}

