package com.modu.office.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(@org.springframework.lang.NonNull StompEndpointRegistry registry) {
        // SockJS fallback을 지원하는 /ws 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(@org.springframework.lang.NonNull MessageBrokerRegistry registry) {
        // 메모리 기반의 SimpleBroker 활성화 (/topic 접두사가 붙은 메시지를 구독하는 클라이언트에게 메시지 전달)
        registry.enableSimpleBroker("/topic");
        // @MessageMapping 어노테이션이 붙은 메서드로 라우팅되는 메시지의 접두사
        registry.setApplicationDestinationPrefixes("/app");
    }
}
