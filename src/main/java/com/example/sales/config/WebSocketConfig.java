package com.example.sales.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.fe.url}")
    String feDomain;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // endpoint FE sẽ kết nối
                .setAllowedOriginPatterns(feDomain) // CORS
                .withSockJS(); // fallback cho trình duyệt cũ
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // đường dẫn server gửi ngược lại
        config.setApplicationDestinationPrefixes("/app"); // tiền tố khi FE gửi
    }
}

