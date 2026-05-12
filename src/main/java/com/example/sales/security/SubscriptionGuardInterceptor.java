// File: src/main/java/com/example/sales/security/SubscriptionGuardInterceptor.java
package com.example.sales.security;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.SubscriptionStatus;
import com.example.sales.constant.UserRole;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.Subscription;
import com.example.sales.repository.SubscriptionRepository;
import com.example.sales.service.ShopContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 1) Shop bị admin khoá ({@code Shop.active == false}): chỉ chặn thao tác <strong>ghi</strong>
 *    (POST/PUT/PATCH/DELETE), gồm đơn hàng và thanh toán. GET/HEAD vẫn xem dữ liệu shop nếu user có quyền.
 *    Ngữ cảnh shop: query {@code shopId}, header {@code X-Shop-Id}, path {@code /api/shops/{id}/...}.
 *    GET không gắn shop (không hint) không suy shop để tránh khoá nhầm toàn session.
 * 2) Chặn ghi khi subscription EXPIRED/CANCELLED (trừ {@code /api/subscription} để gia hạn;
 *    {@code POST /api/shop} tạo shop mới; {@code DELETE /api/shop/{shopId}} xóa shop — không phụ thuộc shop đang chọn / trạng thái khóa).
 * <p>
 * Whitelist toàn cục: {@code /api/auth/**}, {@code /api/user/**} (hồ sơ / mật khẩu — không nên phụ thuộc shop),
 * {@code /api/enums**}, {@code /api/webhook/**}, {@code /api/admin/**},
 * {@code /api/2fa/**}, {@code /api/uploads/**}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionGuardInterceptor implements HandlerInterceptor {

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/api/auth/",
            "/api/user/",
            "/api/enums",
            "/api/webhook/",
            "/api/admin/",
            "/api/2fa/",
            "/api/uploads/"
    );

    private final SubscriptionRepository subscriptionRepository;
    private final ShopContextResolver shopContextResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (isWhitelisted(path)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails user)) {
            return true;
        }

        if (user.getRole() == UserRole.ROLE_ADMIN || user.isImpersonating()) {
            return true;
        }

        // Tạo cửa hàng mới không phụ thuộc shop đang chọn (tránh X-Shop-Id → shop bị khóa chặn POST).
        if (isWriteMethod(method) && "/api/shop".equals(path)) {
            return true;
        }

        // Xóa shop theo path: không dùng X-Shop-Id (có thể là shop khác / đang khóa) để chặn nhầm.
        if (isDeleteShopByIdPath(path, method)) {
            return true;
        }

        String hint = ShopContextResolver.shopIdHintFrom(request);
        boolean explicitShopContext = StringUtils.hasText(hint);

        Shop shop = null;
        if (explicitShopContext || isWriteMethod(method)) {
            shop = shopContextResolver.resolveShopForWriteGuard(user.getId(), hint);
        }

        if (shop != null && !shop.isActive() && isWriteMethod(method)) {
            log.info("[ShopGuard] chặn {} {} shop={} (inactive)", method, path, shop.getId());
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        if (!isWriteMethod(method)) {
            return true;
        }

        if (shop == null) {
            shop = shopContextResolver.resolveShopForWriteGuard(user.getId(), hint);
        }

        if (shop == null) {
            return true;
        }

        if (path != null && path.startsWith("/api/subscription")) {
            return true;
        }

        Subscription sub = subscriptionRepository.findByShopId(shop.getId()).orElse(null);
        if (sub == null) {
            return true;
        }

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
        if (path == null) {
            return false;
        }
        for (String prefix : WHITELIST_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** {@code DELETE /api/shop/{shopId}} — không có thêm segment sau shopId. */
    private boolean isDeleteShopByIdPath(String path, String method) {
        if (!"DELETE".equalsIgnoreCase(method) || path == null) {
            return false;
        }
        if (!path.startsWith("/api/shop/")) {
            return false;
        }
        String rest = path.substring("/api/shop/".length());
        int q = rest.indexOf('?');
        if (q >= 0) {
            rest = rest.substring(0, q);
        }
        if (rest.indexOf('/') >= 0) {
            return false;
        }
        if (!StringUtils.hasText(rest) || "my".equals(rest) || rest.startsWith("by-slug")) {
            return false;
        }
        return true;
    }
}
