// File: src/main/java/com/example/sales/dto/branch/BranchRequest.java
package com.example.sales.dto.branch;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class BranchRequest {

    @NotBlank(message = "Tên chi nhánh không được để trống")
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

    private boolean active = true;
    private boolean isDefault = false;
}
