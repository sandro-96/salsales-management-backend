// File: src/main/java/com/example/sales/dto/admin/AdminImpersonationResponse.java
package com.example.sales.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminImpersonationResponse {
    private String accessToken;
    private long expiresInSeconds;
    private String targetUserId;
    private String targetEmail;
    private String targetRole;
}
