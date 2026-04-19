package com.example.sales.config.ws;

import com.example.sales.cache.ShopUserCache;
import com.example.sales.constant.ShopRole;
import com.example.sales.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChannelInterceptor bảo vệ STOMP endpoint:
 *
 * <ul>
 *   <li><b>CONNECT</b>: bắt buộc có header {@code Authorization: Bearer &lt;jwt&gt;};
 *       parse JWT và gắn {@link StompPrincipal} vào session. JWT sai / thiếu → reject.</li>
 *   <li><b>SUBSCRIBE</b>: kiểm tra quyền theo destination.
 *       <ul>
 *           <li>{@code /topic/shops/{shopId}/...} — user phải là thành viên shop
 *               (tra {@link ShopUserCache}).</li>
 *           <li>{@code /topic/notifications/{userId}} / {@code /topic/verify/{email}} —
 *               cho phép nếu userId/email khớp principal; verify topic cho phép guest.</li>
 *           <li>Destination khác — reject mặc định.</li>
 *       </ul></li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SHOP_TOPIC = Pattern.compile("^/topic/shops/([^/]+)(/.*)?$");
    private static final Pattern NOTIFICATION_TOPIC = Pattern.compile("^/topic/notifications/([^/]+)$");
    private static final Pattern VERIFY_TOPIC = Pattern.compile("^/topic/verify/([^/]+)$");

    private final JwtUtil jwtUtil;
    private final ShopUserCache shopUserCache;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            default -> { /* no-op */ }
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor);
        if (token == null) {
            // Endpoint verify (email) không yêu cầu auth — cho phép anonymous CONNECT;
            // handleSubscribe sẽ chặn mọi destination cần quyền.
            log.debug("STOMP CONNECT without Authorization header — anonymous session");
            return;
        }
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("STOMP CONNECT rejected: invalid JWT");
            throw new MessagingException("Invalid JWT");
        }
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        StompPrincipal principal = new StompPrincipal(userId, role);
        accessor.setUser(principal);
        log.debug("STOMP CONNECT authenticated userId={}, role={}", userId, role);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            throw new MessagingException("Missing destination");
        }

        String userId = principalUserId(accessor);

        // Public: verify topic (không cần auth)
        if (VERIFY_TOPIC.matcher(destination).matches()) {
            return;
        }

        // Các destination còn lại bắt buộc có principal (đã CONNECT với JWT hợp lệ)
        if (userId == null) {
            log.warn("STOMP SUBSCRIBE rejected (no auth): {}", destination);
            throw new MessagingException("Authentication required");
        }

        Matcher notif = NOTIFICATION_TOPIC.matcher(destination);
        if (notif.matches()) {
            if (!userId.equals(notif.group(1))) {
                log.warn("STOMP SUBSCRIBE rejected: user {} tried to subscribe another user's notifications ({})",
                        userId, destination);
                throw new MessagingException("Cannot subscribe another user's notifications");
            }
            return;
        }

        Matcher shop = SHOP_TOPIC.matcher(destination);
        if (shop.matches()) {
            String shopId = shop.group(1);
            if (!isShopMember(shopId, userId)) {
                log.warn("STOMP SUBSCRIBE rejected: user {} not a member of shop {} (dest={})",
                        userId, shopId, destination);
                throw new MessagingException("Not a member of shop " + shopId);
            }
            return;
        }

        // Destination không nằm trong các mẫu cho phép → chặn để tránh rò rỉ.
        log.warn("STOMP SUBSCRIBE rejected (unsupported destination): {}", destination);
        throw new MessagingException("Destination not allowed: " + destination);
    }

    private String principalUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompPrincipal p) {
            return p.userId();
        }
        if (accessor.getUser() != null && StringUtils.hasText(accessor.getUser().getName())) {
            return accessor.getUser().getName();
        }
        return null;
    }

    private boolean isShopMember(String shopId, String userId) {
        try {
            ShopRole role = shopUserCache.getUserRoleInShop(shopId, userId);
            return role != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("Authorization");
        if (values == null || values.isEmpty()) {
            return null;
        }
        String header = values.get(0);
        if (header == null) return null;
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return header.trim();
    }
}
