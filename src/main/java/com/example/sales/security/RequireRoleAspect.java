// File: src/main/java/com/example/sales/security/RequireRoleAspect.java
package com.example.sales.security;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequireRoleAspect {

    private final PermissionChecker permissionChecker;

    @Before("@annotation(com.example.sales.security.RequireRole)")
    public void checkPermission(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        ShopRole[] roles = requireRole.value();

        User user = null;
        String shopId = null;

        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < args.length; i++) {
            if (user == null && Arrays.stream(paramAnnotations[i])
                    .anyMatch(a -> a.annotationType() == AuthenticationPrincipal.class)) {
                user = (User) args[i];
            }
            if (shopId == null && args[i] instanceof String str && str.startsWith("shop_")) {
                shopId = str;
            }
        }

        if (user == null || shopId == null) {
            log.warn("Thiếu user hoặc shopId khi kiểm tra quyền cho method: {}", method.getName());
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        boolean allowed = permissionChecker.hasRole(shopId, user.getId(), roles);
        if (!allowed) {
            log.warn("Người dùng {} bị từ chối truy cập shop {} với vai trò yêu cầu: {}",
                    user.getId(), shopId, Arrays.toString(roles));
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }
}