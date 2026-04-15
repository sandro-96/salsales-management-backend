package com.example.sales.dto.tableGroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableGroupResponse {
    private String id;
    private String shopId;
    private String branchId;
    private String name;
    private List<String> tableIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

