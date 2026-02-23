package com.quickflux.websocket.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {

    // userId -> WebSocketSession
    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(UUID userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.info("WebSocket session added for user: {} (total sessions: {})", userId, sessions.size());
    }

    public void removeSession(UUID userId) {
        sessions.remove(userId);
        log.info("WebSocket session removed for user: {} (total sessions: {})", userId, sessions.size());
    }

    public void sendMessageToUser(UUID userId, String message) {
        WebSocketSession session = sessions.get(userId);

        if (session == null) {
            log.debug("No WebSocket session found for user: {}", userId);
            return;
        }

        if (!session.isOpen()) {
            log.warn("WebSocket session for user {} is closed, removing", userId);
            sessions.remove(userId);
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
            log.debug("Message sent to user {}: {}", userId, message);
        } catch (IOException e) {
            log.error("Error sending message to user {}: {}", userId, e.getMessage());
            sessions.remove(userId);
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public Map<UUID, WebSocketSession> getAllSessions() {
        return new HashMap<>(sessions);
    }
}