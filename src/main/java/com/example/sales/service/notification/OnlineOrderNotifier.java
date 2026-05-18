// File: src/main/java/com/example/sales/service/notification/OnlineOrderNotifier.java
package com.example.sales.service.notification;

import com.example.sales.constant.WebSocketMessageType;
import com.example.sales.model.Order;
import com.example.sales.model.Shop;
import com.example.sales.model.Table;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import com.example.sales.service.MailService;
import com.example.sales.service.realtime.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Thông báo đến shop owner khi có đơn online mới:
 *  - WebSocket: push lên topic {@code /topic/shops/{shopId}/orders/online} với type ONLINE_ORDER_CREATED.
 *  - Email: gửi async cho owner (best-effort, không throw nếu lỗi).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineOrderNotifier {

    private final RealtimeEventPublisher realtimeEventPublisher;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${app.fe.url:}")
    private String feUrl;

    /**
     * Đẩy WS đồng bộ (nhanh) + gửi email không chặn flow tạo đơn.
     * Chạy {@code @Async} ở mức entry point để đảm bảo email gửi trên thread riêng,
     * tránh self-invocation pitfall của Spring AOP.
     */
    @Async
    public void notifyOnlineOrderCreated(Shop shop, Order order) {
        publishWebSocketEvent(shop, order);
        sendEmailToOwner(shop, order, /* tableName */ null);
    }

    /**
     * Thông báo đơn IN_STORE (khách quét QR tại bàn) cho owner: cùng channel WS như
     * đơn online + email với prefix "[Đơn tại bàn ...]" để phân biệt.
     */
    @Async
    public void notifyInStoreOrderCreated(Shop shop, Order order, Table table) {
        publishWebSocketEvent(shop, order);
        sendEmailToOwner(shop, order, table != null ? table.getName() : null);
    }

    private void publishWebSocketEvent(Shop shop, Order order) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getId());
            payload.put("orderCode", order.getOrderCode());
            payload.put("shopId", shop.getId());
            payload.put("branchId", order.getBranchId());
            payload.put("tableId", order.getTableId());
            payload.put("customerName", order.getGuestName());
            payload.put("customerPhone", order.getGuestPhone());
            payload.put("totalAmount", order.getTotalAmount());
            payload.put("status", order.getStatus() != null ? order.getStatus().name() : null);
            payload.put("orderSource", order.getOrderSource() != null ? order.getOrderSource().name() : null);
            realtimeEventPublisher.publishShopEvent(
                    shop.getId(), "orders/online",
                    WebSocketMessageType.ONLINE_ORDER_CREATED, payload);
        } catch (Exception ex) {
            log.warn("Failed publishing ONLINE_ORDER_CREATED WS event (shop={}, order={}): {}",
                    shop.getId(), order.getId(), ex.getMessage());
        }
    }

    private void sendEmailToOwner(Shop shop, Order order, String tableName) {
        if (!StringUtils.hasText(shop.getOwnerId())) {
            return;
        }
        try {
            User owner = userRepository.findByIdAndDeletedFalse(shop.getOwnerId()).orElse(null);
            if (owner == null || !StringUtils.hasText(owner.getEmail())) {
                return;
            }

            boolean isInStore = StringUtils.hasText(tableName);
            String subject = isInStore
                    ? "Đơn tại bàn " + tableName + " #" + order.getOrderCode()
                    : "Đơn hàng online mới #" + order.getOrderCode();

            Map<String, Object> model = new HashMap<>();
            model.put("ownerName", StringUtils.hasText(owner.getEmail()) ? owner.getEmail() : "Chủ shop");
            model.put("shopName", shop.getName());
            model.put("orderCode", order.getOrderCode());
            model.put("customerName", order.getGuestName());
            model.put("customerPhone", order.getGuestPhone());
            model.put("totalAmount", formatCurrency(order.getTotalAmount(), shop.getCurrency()));
            model.put("addressNote", order.getNote() == null ? "" : order.getNote());
            model.put("orderUrl", buildOrderUrl(order.getId()));
            model.put("tableName", tableName == null ? "" : tableName);

            mailService.sendHtmlTemplate(owner.getEmail(), subject,
                    "emails/online-order-created",
                    model);
        } catch (Exception ex) {
            log.warn("Failed sending order email (shop={}, order={}): {}",
                    shop.getId(), order.getId(), ex.getMessage());
        }
    }

    private String buildOrderUrl(String orderId) {
        String base = feUrl == null ? "" : feUrl.replaceAll("/+$", "");
        return base + "/orders?orderId=" + (orderId == null ? "" : orderId);
    }

    private String formatCurrency(double amount, String currency) {
        try {
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formatted = nf.format(Math.round(amount));
            return formatted + " " + (StringUtils.hasText(currency) ? currency : "VND");
        } catch (Exception ex) {
            return amount + " " + (StringUtils.hasText(currency) ? currency : "VND");
        }
    }
}
