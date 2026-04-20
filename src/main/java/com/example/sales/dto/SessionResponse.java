// File: src/main/java/com/example/sales/dto/SessionResponse.java
package com.example.sales.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionResponse {
    private String userId;
    private String email;
    private String role;
    private boolean impersonating;
    private String impersonatedBy;
    private String impersonatorEmail;
}
