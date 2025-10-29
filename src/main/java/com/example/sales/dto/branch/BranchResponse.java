// File: src/main/java/com/example/sales/dto/branch/BranchResponse.java
package com.example.sales.dto.branch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchResponse {
    private String id;
    private String name;
    private String address;
    private String phone;
    private boolean active;
    private boolean isDefault;
    private String createdAt;
}
