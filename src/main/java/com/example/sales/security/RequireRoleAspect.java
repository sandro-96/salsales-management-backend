// File: `src/main/java/com/example/sales/security/RequireRoleAspect.java`
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
import java.lang.reflect.Parameter;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequireRoleAspect {

    private final PermissionChecker permissionChecker;

    @Before("@annotation(com.example.sales.security.RequireRole)")
    public void checkPermission(JoinPoint joinPoint) {
        Method method;
        try {
            method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            if (method.getDeclaringClass().isInterface()) {
                method = joinPoint.getTarget().getClass()
                        .getMethod(method.getName(), method.getParameterTypes());
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to resolve target method for permission check", e);
        }

        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        ShopRole[] roles = requireRole.value();

        CustomUserDetails user = null;
        String shopId = null;

        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Parameter parameter = (i < parameters.length) ? parameters[i] : null;

            for (Annotation annotationParam : paramAnnotations[i]) {
                if (annotationParam instanceof AuthenticationPrincipal) {
                    if (arg instanceof CustomUserDetails) {
                        user = (CustomUserDetails) arg;
                    }
                }

                if (annotationParam instanceof PathVariable pv) {
                    String name = !pv.value().isEmpty() ? pv.value()
                            : !pv.name().isEmpty() ? pv.name()
                            : (parameter != null ? parameter.getName() : "");
                    if ("shopId".equals(name)) {
                        if (arg != null) {
                            shopId = String.valueOf(arg);
                        }
                    }
                }

                if (annotationParam instanceof RequestParam rp) {
                    String name = !rp.value().isEmpty() ? rp.value()
                            : !rp.name().isEmpty() ? rp.name()
                            : (parameter != null ? parameter.getName() : "");
                    if ("shopId".equals(name)) {
                        if (arg != null) {
                            shopId = String.valueOf(arg);
                        }
                    }
                }
            }
        }

        if (user == null || shopId == null) {
            log.warn("Missing user or shopId when checking permission for method: {}", method.getName());
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        boolean allowed = permissionChecker.hasRole(shopId, user.getId(), roles);
        if (!allowed) {
            log.warn("User {} denied access to shop {} for required roles: {}", user.getId(), shopId, Arrays.toString(roles));
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }
}