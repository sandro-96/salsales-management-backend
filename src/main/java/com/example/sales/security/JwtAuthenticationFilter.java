// File: src/main/java/com/example/sales/security/JwtAuthenticationFilter.java
package com.example.sales.security;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.UserRole;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("JWT không hợp lệ hoặc hết hạn: {}", token);
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = jwtUtil.extractUserId(token);
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            // Chỉ load admin permissions từ DB khi role = ROLE_ADMIN. User thường
            // bỏ qua để tránh DB hit cho mỗi request; admin endpoint mới cần.
            Set<AdminPermission> adminPerms = EnumSet.noneOf(AdminPermission.class);
            if (UserRole.ROLE_ADMIN.name().equals(role)) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent() && userOpt.get().getAdminPermissions() != null
                        && !userOpt.get().getAdminPermissions().isEmpty()) {
                    adminPerms = EnumSet.copyOf(userOpt.get().getAdminPermissions());
                }
            }

            CustomUserDetails userDetails = new CustomUserDetails(
                    userId,
                    email,
                    null,
                    UserRole.valueOf(role),
                    adminPerms,
                    List.of(new SimpleGrantedAuthority(role))
            );
            String impersonatedBy = jwtUtil.extractImpersonatedBy(token);
            if (impersonatedBy != null && !impersonatedBy.isBlank()) {
                userDetails.setImpersonatedBy(impersonatedBy);
                userDetails.setImpersonatorEmail(jwtUtil.extractImpersonatorEmail(token));
            }
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(userDetails);

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("JWT hợp lệ. Gán userId = {}, role = {}", userId, role);
        }

        filterChain.doFilter(request, response);
    }
}
