package com.quickflux.websocket.config;

import com.quickflux.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketHeartbeat {

    private final WebSocketSessionManager sessionManager;

    @Scheduled(fixedRate = 30000)  // Every 30 seconds
    public void sendHeartbeat() {
        log.debug("Sending WebSocket heartbeat to {} sessions",
                sessionManager.getActiveSessionCount());

        sessionManager.getAllSessions().forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage());
                } catch (Exception e) {
                    log.error("Error sending ping to user {}: {}", userId, e.getMessage());
                }
            }
        });
    }
}