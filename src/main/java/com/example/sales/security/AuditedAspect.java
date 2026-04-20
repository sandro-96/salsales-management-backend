// File: src/main/java/com/example/sales/security/AuditedAspect.java
package com.example.sales.security;

import com.example.sales.model.AuditLog;
import com.example.sales.service.audit.AdminAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Order(200)
@Component
@RequiredArgsConstructor
public class AuditedAspect {

    private static final ParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final AdminAuditService adminAuditService;

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        boolean success = false;
        String errorMessage = null;
        try {
            result = pjp.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                writeEntry(pjp, audited, success, errorMessage, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("Audit aspect failed: {}", ex.getMessage());
            }
        }
    }

    private void writeEntry(ProceedingJoinPoint pjp, Audited audited,
                            boolean success, String errorMessage, long durationMs) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Object[] args = pjp.getArgs();

        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                null, method, args, PARAM_DISCOVERER
        );

        String targetId = evalString(audited.targetIdExpr(), ctx);
        String targetLabel = evalString(audited.targetLabelExpr(), ctx);

        AuditActor actor = currentActor();
        HttpServletRequest request = currentRequest();

        Map<String, Object> meta = new HashMap<>();
        meta.put("method", method.getName());
        meta.put("durationMs", durationMs);

        AuditLog entry = AuditLog.builder()
                .actorId(actor.id())
                .actorEmail(actor.email())
                .resource(audited.resource())
                .action(audited.action())
                .targetId(targetId)
                .targetLabel(targetLabel)
                .metadata(meta)
                .ip(request != null ? clientIp(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .status(success ? "SUCCESS" : "FAIL")
                .errorMessage(errorMessage)
                .build();
        adminAuditService.record(entry);
    }

    private String evalString(String expr, MethodBasedEvaluationContext ctx) {
        if (!StringUtils.hasText(expr)) return null;
        try {
            Expression e = PARSER.parseExpression(expr);
            Object v = e.getValue(ctx);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ex) {
            return null;
        }
    }

    private AuditActor currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return new AuditActor(null, null);
        if (auth.getPrincipal() instanceof CustomUserDetails u) {
            return new AuditActor(u.getId(), u.getUsername());
        }
        return new AuditActor(null, auth.getName());
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest();
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) return ip.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private record AuditActor(String id, String email) {}
}
