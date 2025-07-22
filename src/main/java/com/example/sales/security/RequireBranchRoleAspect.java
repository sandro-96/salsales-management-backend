// File: src/main/java/com/example/sales/security/RequireBranchRoleAspect.java
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequireBranchRoleAspect {

    private final PermissionChecker permissionChecker;

    @Before("@annotation(requireBranchRole)")
    public void check(JoinPoint joinPoint, RequireBranchRole requireBranchRole) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        CustomUserDetails user = null;
        String branchId = null;

        for (int i = 0; i < args.length; i++) {
            // Tìm CustomUserDetails từ @AuthenticationPrincipal
            if (user == null && Arrays.stream(paramAnnotations[i])
                    .anyMatch(a -> a.annotationType() == AuthenticationPrincipal.class)) {
                user = (CustomUserDetails) args[i];
            }

            // Ưu tiên: Tìm tham số tên branchId rõ ràng
            if (branchId == null && paramAnnotations[i] != null) {
                for (Annotation annotation : paramAnnotations[i]) {
                    if (annotation instanceof org.springframework.web.bind.annotation.PathVariable pathVar &&
                            "branchId".equals(pathVar.value())) {
                        assert args[i] instanceof String;
                        branchId = (String) args[i];
                        break;
                    }
                    if (annotation instanceof org.springframework.web.bind.annotation.RequestParam reqParam &&
                            "branchId".equals(reqParam.value())) {
                        assert args[i] instanceof String;
                        branchId = (String) args[i];
                        break;
                    }
                }
            }
        }

        if (user == null || branchId == null) {
            log.warn("Thiếu user hoặc branchId trong @RequireBranchRole - method: {}", method.getName());
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        ShopRole[] allowedRoles = requireBranchRole.value();
        boolean hasRole = permissionChecker.hasBranchRole(branchId, user.getId(), allowedRoles);
        if (!hasRole) {
            log.warn("Người dùng {} bị từ chối truy cập branch {} với vai trò: {}",
                    user.getId(), branchId, Arrays.toString(allowedRoles));
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }
}
