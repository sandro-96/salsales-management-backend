// File: src/main/java/com/example/sales/security/CustomUserDetails.java
package com.example.sales.security;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.UserRole;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@Getter
public class CustomUserDetails implements UserDetails {

    private final String id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final Set<AdminPermission> adminPermissions;
    private final Collection<? extends GrantedAuthority> authorities;

    /** Khi session này là impersonation, id của admin gốc. Null nếu là phiên thật. */
    @Setter
    private String impersonatedBy;
    @Setter
    private String impersonatorEmail;

    public CustomUserDetails(String id,
                             String email,
                             String password,
                             UserRole role,
                             Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, role, EnumSet.noneOf(AdminPermission.class), authorities);
    }

    public CustomUserDetails(String id,
                             String email,
                             String password,
                             UserRole role,
                             Set<AdminPermission> adminPermissions,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.adminPermissions = adminPermissions == null
                ? EnumSet.noneOf(AdminPermission.class)
                : EnumSet.copyOf(adminPermissions);
        this.authorities = authorities;
    }

    public boolean hasAdminPermission(AdminPermission permission) {
        return adminPermissions != null && adminPermissions.contains(permission);
    }

    public boolean isImpersonating() {
        return impersonatedBy != null && !impersonatedBy.isBlank();
    }

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
