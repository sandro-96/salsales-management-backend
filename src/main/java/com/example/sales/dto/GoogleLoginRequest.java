package com.example.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "ID token không được để trống")
    private String idToken;
}