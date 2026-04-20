// File: src/main/java/com/example/sales/security/RequireAdminPermissionAspect.java
package com.example.sales.security;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.UserRole;
import com.example.sales.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Kiểm {@link RequireAdminPermission}: principal phải là {@link UserRole#ROLE_ADMIN}
 * và {@code adminPermissions} phải chứa quyền yêu cầu. Nếu class method nằm trong
 * {@code /api/admin/**} nhưng không có annotation, aspect này bỏ qua — mặc định
 * vẫn còn gate {@code hasRole("ADMIN")} từ {@code SecurityConfig}.
 */
@Aspect
@Component
@Slf4j
public class RequireAdminPermissionAspect {

    @Before("@annotation(com.example.sales.security.RequireAdminPermission) "
            + "|| @within(com.example.sales.security.RequireAdminPermission)")
    public void check(JoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            method = joinPoint.getTarget().getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
        }

        RequireAdminPermission annotation = method.getAnnotation(RequireAdminPermission.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass()
                    .getAnnotation(RequireAdminPermission.class);
        }
        if (annotation == null) {
            return;
        }
        AdminPermission required = annotation.value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            log.warn("[AdminPerm] anonymous access denied (required={})", required);
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        if (user.getRole() != UserRole.ROLE_ADMIN) {
            log.warn("[AdminPerm] user {} role={} không phải admin, từ chối {}",
                    user.getId(), user.getRole(), required);
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        if (!user.hasAdminPermission(required)) {
            log.warn("[AdminPerm] admin {} thiếu quyền {} (có: {})",
                    user.getId(), required, user.getAdminPermissions());
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }
}
