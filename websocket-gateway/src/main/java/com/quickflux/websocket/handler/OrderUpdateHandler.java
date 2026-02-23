package com.quickflux.websocket.handler;

import com.quickflux.websocket.security.JwtService;
import com.quickflux.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderUpdateHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("New WebSocket connection: {}", session.getId());

        // Extract JWT from query parameter
        URI uri = session.getUri();
        if (uri == null) {
            log.warn("No URI in WebSocket session");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String query = uri.getQuery();
        if (query == null || !query.startsWith("token=")) {
            log.warn("No token in WebSocket connection");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String token = query.substring(6); // Remove "token="

        // Validate JWT
        if (!jwtService.isTokenValid(token)) {
            log.warn("Invalid JWT token in WebSocket connection");
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // Extract user ID
        UUID userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);

        // Store session
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("email", email);
        sessionManager.addSession(userId, session);

        // Send welcome message
        session.sendMessage(new TextMessage("{\"type\":\"connected\",\"message\":\"WebSocket connected for user " + email + "\"}"));

        log.info("WebSocket authenticated for user: {} ({})", email, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received message from session {}: {}", session.getId(), message.getPayload());

        // Echo back for testing (optional)
        // session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID userId = (UUID) session.getAttributes().get("userId");

        if (userId != null) {
            sessionManager.removeSession(userId);
            log.info("WebSocket connection closed for user: {} (status: {})", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }
}