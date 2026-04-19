package com.example.sales.config.ws;

import java.security.Principal;

/**
 * Principal tối giản gắn vào STOMP session sau khi CONNECT xác thực JWT thành công.
 * Cho phép sau này dùng {@code convertAndSendToUser(userId, ...)} để push event
 * cá nhân không đoán được trên client khác.
 */
public record StompPrincipal(String userId, String role) implements Principal {
    @Override
    public String getName() {
        return userId;
    }
}
