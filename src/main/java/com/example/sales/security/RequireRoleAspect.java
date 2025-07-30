// File: src/main/java/com/example/sales/security/RequireRoleAspect.java
package com.example.sales.security;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
    public void checkPermission(JoinPoint joinPoint) throws NoSuchMethodException {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (method.getDeclaringClass().isInterface()) {
            method = joinPoint.getTarget().getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
        }
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        ShopRole[] roles = requireRole.value();

        CustomUserDetails user = null;
        String shopId = null;

        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < args.length; i++) {
            for (Annotation annotationParam : paramAnnotations[i]) {
                if (annotationParam instanceof AuthenticationPrincipal) {
                    assert args[i] instanceof CustomUserDetails;
                    user = (CustomUserDetails) args[i];
                }
                if (annotationParam instanceof PathVariable pv) {
                    if ("shopId".equals(pv.value()) || pv.value().isEmpty()) {
                        assert args[i] instanceof String;
                        shopId = (String) args[i];
                    }
                }
                if (annotationParam instanceof RequestParam rp) {
                    if ("shopId".equals(rp.value())) {
                        assert args[i] instanceof String;
                        shopId = (String) args[i];
                    }
                }
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