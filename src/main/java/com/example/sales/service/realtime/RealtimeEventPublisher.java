package com.example.sales.service.realtime;

import com.example.sales.constant.WebSocketMessageType;
import com.example.sales.dto.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Điểm vào duy nhất để producer push event realtime tới client.
 *
 * <p>Convention destination:
 * <ul>
 *   <li>{@code /topic/shops/{shopId}/branches/{branchId}/orders} — event lifecycle đơn hàng</li>
 *   <li>{@code /topic/shops/{shopId}/branches/{branchId}/tables} — event trạng thái bàn</li>
 *   <li>{@code /topic/shops/{shopId}/branches/{branchId}/payments} — event thanh toán</li>
 *   <li>{@code /topic/shops/{shopId}/{domain}} — event cấp shop (không gắn branch)</li>
 * </ul>
 *
 * <p>Publish là best-effort: nếu broker gặp lỗi thì log warn, không throw để
 * tránh làm fail transaction gốc (DB đã commit trước khi publish).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public <T> void publishOrderEvent(String shopId, String branchId,
                                      WebSocketMessageType type, T payload) {
        publishBranch(shopId, branchId, "orders", type, payload);
    }

    public <T> void publishTableEvent(String shopId, String branchId,
                                      WebSocketMessageType type, T payload) {
        publishBranch(shopId, branchId, "tables", type, payload);
    }

    public <T> void publishPaymentEvent(String shopId, String branchId,
                                        WebSocketMessageType type, T payload) {
        publishBranch(shopId, branchId, "payments", type, payload);
    }

    /**
     * Dành cho sự kiện cấp shop (không thuộc branch cụ thể) — ví dụ đổi cài đặt.
     */
    public <T> void publishShopEvent(String shopId, String domain,
                                     WebSocketMessageType type, T payload) {
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(domain)) {
            return;
        }
        publish("/topic/shops/" + shopId + "/" + domain, type, payload);
    }

    private <T> void publishBranch(String shopId, String branchId, String domain,
                                   WebSocketMessageType type, T payload) {
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(branchId)) {
            log.debug("Skip realtime publish ({}): missing shopId/branchId", type);
            return;
        }
        publish("/topic/shops/" + shopId + "/branches/" + branchId + "/" + domain,
                type, payload);
    }

    private <T> void publish(String destination, WebSocketMessageType type, T payload) {
        try {
            messagingTemplate.convertAndSend(destination,
                    new WebSocketMessage<>(type, payload));
        } catch (Exception ex) {
            log.warn("Realtime publish failed ({} -> {}): {}", type, destination, ex.getMessage());
        }
    }
}
