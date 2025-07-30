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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequirePermissionAspect {

    private final PermissionChecker permissionChecker;

    @Before("@annotation(com.example.sales.security.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (method.getDeclaringClass().isInterface()) {
            method = joinPoint.getTarget().getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
        }

        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        Permission requiredPermission = annotation.value();

        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        ExtractedParams extracted = extractParams(args, paramAnnotations, signature);

        if (extracted.user == null) {
            log.warn("Thiếu thông tin người dùng: user={}", extracted.user);
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        if (extracted.shopId == null) {
            log.warn("Thiếu tham số shopId: shopId={}", extracted.shopId);
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        boolean hasPermission = permissionChecker.hasPermission(
                extracted.shopId, extracted.branchId, extracted.user.getId(), requiredPermission);

        if (!hasPermission) {
            log.warn("User {} bị từ chối quyền {} tại shop={}, branch={}",
                    extracted.user.getId(), requiredPermission, extracted.shopId, extracted.branchId);
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }

    private ExtractedParams extractParams(Object[] args, Annotation[][] paramAnnotations, MethodSignature signature) {
        CustomUserDetails user = null;
        String shopId = null;
        String branchId = null;

        // Lấy tên tham số từ MethodSignature
        String[] paramNames = signature.getParameterNames();

        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation instanceof AuthenticationPrincipal && args[i] instanceof CustomUserDetails details) {
                    user = details;
                } else if (annotation instanceof PathVariable pv) {
                    String value = pv.value();
                    // Kiểm tra cả value của @PathVariable và tên tham số
                    if ("shopId".equals(value) || (value.isEmpty() && "shopId".equals(paramNames[i]))) {
                        shopId = args[i] != null ? String.valueOf(args[i]) : null;
                    } else if ("branchId".equals(value) || (value.isEmpty() && "branchId".equals(paramNames[i]))) {
                        branchId = args[i] != null ? String.valueOf(args[i]) : null;
                    }
                } else if (annotation instanceof RequestParam rp) {
                    String value = rp.value();
                    // Kiểm tra cả value của @RequestParam và tên tham số
                    if ("shopId".equals(value) || (value.isEmpty() && "shopId".equals(paramNames[i]))) {
                        shopId = args[i] != null ? String.valueOf(args[i]) : null;
                    } else if ("branchId".equals(value) || (value.isEmpty() && "branchId".equals(paramNames[i]))) {
                        branchId = args[i] != null ? String.valueOf(args[i]) : null;
                    }
                }
            }
        }

        // Log chi tiết để debug
        log.debug("Extracted params: user={}, shopId={}, branchId={}",
                user != null ? user.getId() : null, shopId, branchId);

        return new ExtractedParams(user, shopId, branchId);
    }

    private record ExtractedParams(CustomUserDetails user, String shopId, String branchId) {
    }
}