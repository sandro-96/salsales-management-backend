// File: src/main/java/com/example/sales/dto/branch/BranchDetailResponse.java
package com.example.sales.dto.branch;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class BranchDetailResponse {

    private String id;
    private String shopId;
    private String slug;
    private String name;
    private String address;
    private String phone;

    private LocalDate openingDate;
    private LocalTime openingTime;
    private LocalTime closingTime;

    private String managerName;
    private String managerPhone;

    private Integer capacity;
    private String description;

    private boolean active;
    private boolean isDefault;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
