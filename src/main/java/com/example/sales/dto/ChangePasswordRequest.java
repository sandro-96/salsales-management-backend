// File: src/main/java/com/example/sales/dto/ChangePasswordRequest.java
package com.example.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}