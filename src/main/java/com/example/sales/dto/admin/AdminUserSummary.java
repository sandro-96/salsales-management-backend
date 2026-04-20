// File: src/main/java/com/example/sales/dto/admin/AdminUserSummary.java
package com.example.sales.dto.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserSummary {
    private String id;
    private String email;
    private String fullName;
    private UserRole role;
    private boolean active;
    private boolean verified;
    private Instant lastLoginAt;
    private LocalDateTime createdAt;
    private Set<AdminPermission> adminPermissions;
    private long ownedShopCount;
    private long memberShopCount;
}
