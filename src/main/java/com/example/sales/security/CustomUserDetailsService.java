// File: src/main/java/com/example/sales/security/CustomUserDetailsService.java
package com.example.sales.security;

import com.example.sales.constant.AdminPermission;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Set<AdminPermission> adminPerms = user.getAdminPermissions() == null
                || user.getAdminPermissions().isEmpty()
                ? EnumSet.noneOf(AdminPermission.class)
                : EnumSet.copyOf(user.getAdminPermissions());
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                adminPerms,
                List.of(new SimpleGrantedAuthority("" + user.getRole()))
        );
    }
}

