package com.example.sales.config;

import com.example.sales.config.ws.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final FrontendCorsProperties frontendCors;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // endpoint FE sẽ kết nối
                .setAllowedOriginPatterns(frontendCors.allowedOriginPatterns().toArray(String[]::new))
                .withSockJS(); // fallback cho trình duyệt cũ
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // bật thêm /queue cho user-destination
        config.setApplicationDestinationPrefixes("/app"); // tiền tố khi FE gửi
        // Cho phép sau này dùng SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/...")
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
