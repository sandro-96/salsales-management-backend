// File: src/main/java/com/example/sales/dto/UpdateProfileRequest.java
package com.example.sales.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String businessType;
}