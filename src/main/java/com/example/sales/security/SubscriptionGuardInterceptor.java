// File: src/main/java/com/example/sales/security/SubscriptionGuardInterceptor.java
package com.example.sales.security;

import com.example.sales.cache.ShopCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.constant.UserRole;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Khóa mọi thao tác ghi (POST/PUT/PATCH/DELETE) khi subscription của shop hiện tại
 * đang ở trạng thái {@link SubscriptionStatus#EXPIRED} hoặc {@link SubscriptionStatus#CANCELLED}.
 * <p>
 * Whitelist các endpoint cần còn hoạt động:
 * <ul>
 *   <li>{@code /api/auth/**}: đăng nhập/đăng xuất.</li>
 *   <li>{@code /api/subscription/**}: xem trạng thái + tạo thanh toán.</li>
 *   <li>{@code /api/webhook/**}: callback của cổng thanh toán.</li>
 *   <li>{@code /api/admin/**}: admin luôn có thể thao tác (admin không có shop riêng).</li>
 *   <li>Impersonation session: admin đang mượn danh user cũng được qua.</li>
 * </ul>
 * GET luôn được phép — người dùng xem lịch sử để tra cứu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionGuardInterceptor implements HandlerInterceptor {

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/api/auth/",
            "/api/subscription/",
            "/api/webhook/",
            "/api/admin/",
            "/api/2fa/",
            "/api/uploads/"
    );

    private final SubscriptionRepository subscriptionRepository;
    private final ShopCache shopCache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if (!isWriteMethod(method)) return true;

        String path = request.getRequestURI();
        if (isWhitelisted(path)) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return true; // để AuthFilter xử lý
        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails user)) return true;

        // Admin (kể cả đang impersonate) không bị guard — admin không vận hành POS.
        if (user.getRole() == UserRole.ROLE_ADMIN || user.isImpersonating()) return true;

        Shop shop;
        try {
            shop = shopCache.getShopByOwner(user.getId());
        } catch (BusinessException ex) {
            // User không gắn với shop nào (admin cũ, hoặc user staff không có shop) → không chặn.
            return true;
        }

        Subscription sub = subscriptionRepository.findByShopId(shop.getId()).orElse(null);
        if (sub == null) return true; // chưa có subscription → coi như TRIAL, cho qua.

        if (sub.getStatus() == SubscriptionStatus.EXPIRED
                || sub.getStatus() == SubscriptionStatus.CANCELLED) {
            log.info("[SubscriptionGuard] chặn {} {} shop={} status={}",
                    method, path, shop.getId(), sub.getStatus());
            throw new BusinessException(ApiCode.SUBSCRIPTION_EXPIRED);
        }
        return true;
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean isWhitelisted(String path) {
        if (path == null) return false;
        for (String prefix : WHITELIST_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }
}
