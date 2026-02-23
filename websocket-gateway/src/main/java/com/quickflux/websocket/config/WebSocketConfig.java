package com.quickflux.websocket.config;

import com.quickflux.websocket.handler.OrderUpdateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderUpdateHandler orderUpdateHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderUpdateHandler, "/ws/orders")
                .setAllowedOrigins("*"); // allowing all for testing
//                .setAllowedOrigins("http://localhost:3000", "http://localhost:8080");
    }
}