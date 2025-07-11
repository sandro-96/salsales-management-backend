package com.example.sales.dto.branch;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchRequest {

    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String name;

    private String address;

    private String phone;

    private boolean active = true;
}
