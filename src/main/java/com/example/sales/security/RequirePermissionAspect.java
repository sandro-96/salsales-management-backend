// File: src/main/java/com/example/sales/security/RequirePermissionAspect.java
package com.example.sales.security;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.exception.BusinessException;
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

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequirePermissionAspect {

    private final PermissionChecker permissionChecker;

    @Before("@annotation(com.example.sales.security.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        Permission requiredPermission = annotation.value();

        CustomUserDetails user = null;
        String shopId = null;
        String branchId = null;

        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < args.length; i++) {
            for (Annotation annotationParam : paramAnnotations[i]) {
                if (annotationParam instanceof AuthenticationPrincipal) {
                    user = (CustomUserDetails) args[i];
                }
                if (annotationParam instanceof org.springframework.web.bind.annotation.PathVariable pv) {
                    if ("shopId".equals(pv.value())) {
                        shopId = (String) args[i];
                    } else if ("branchId".equals(pv.value())) {
                        branchId = (String) args[i];
                    }
                }
                if (annotationParam instanceof org.springframework.web.bind.annotation.RequestParam rp) {
                    if ("shopId".equals(rp.value())) {
                        shopId = (String) args[i];
                    } else if ("branchId".equals(rp.value())) {
                        branchId = (String) args[i];
                    }
                }
            }
        }

        if (user == null || shopId == null || branchId == null) {
            log.warn("Thiếu tham số khi kiểm tra @RequirePermission: user={}, shopId={}, branchId={}", user, shopId, branchId);
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        boolean hasPermission = permissionChecker.hasPermission(shopId, branchId, user.getId(), requiredPermission);
        if (!hasPermission) {
            log.warn("Người dùng {} KHÔNG có quyền {} tại shop={}, branch={}", user.getId(), requiredPermission, shopId, branchId);
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }
}
