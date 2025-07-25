// File: src/main/java/com/example/sales/security/CustomUserDetails.java
package com.example.sales.security;

import com.example.sales.constant.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final String id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

}
