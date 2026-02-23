package com.quickflux.websocket.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickflux.contracts.events.OrderCreatedV1;
import com.quickflux.contracts.events.PaymentCapturedV1;
import com.quickflux.contracts.events.PaymentFailedV1;
import com.quickflux.contracts.events.StockConfirmedV1;
import com.quickflux.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order.created", groupId = "websocket-gateway")
    public void handleOrderCreated(OrderCreatedV1 event) {
        log.info("Received OrderCreated event for order {}, user {}",
                event.orderId(), event.userId());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "ORDER_CREATED");
        message.put("orderId", event.orderId().toString());
        message.put("status", "PENDING");
        message.put("timestamp", event.timestamp().toString());

        sendToUser(event.userId(), message);
    }

    @KafkaListener(topics = "payment.captured", groupId = "websocket-gateway")
    public void handlePaymentCaptured(PaymentCapturedV1 event) {
        log.info("Received PaymentCaptured event for order {}", event.orderId());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "PAYMENT_CAPTURED");
        message.put("orderId", event.orderId().toString());
        message.put("status", "PAYMENT_COMPLETED");
        message.put("timestamp", event.timestamp().toString());

        sendToUser(event.userId(), message);
    }

    @KafkaListener(topics = "stock.confirmed", groupId = "websocket-gateway")
    public void handleStockConfirmed(StockConfirmedV1 event) {
        log.info("Received StockConfirmed event for order {}", event.orderId());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "ORDER_CONFIRMED");
        message.put("orderId", event.orderId().toString());
        message.put("status", "CONFIRMED");
        message.put("timestamp", event.timestamp().toString());

        sendToUser(event.userId(), message);
    }

    @KafkaListener(topics = "payment.failed", groupId = "websocket-gateway")
    public void handlePaymentFailed(PaymentFailedV1 event) {
        log.info("Received PaymentFailed event for order {}", event.orderId());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "ORDER_FAILED");
        message.put("orderId", event.orderId().toString());
        message.put("status", "CANCELLED");
        message.put("reason", event.reason());
        message.put("timestamp", event.timestamp().toString());

        sendToUser(event.userId(), message);
    }

    private void sendToUser(java.util.UUID userId, Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sessionManager.sendMessageToUser(userId, json);
        } catch (Exception e) {
            log.error("Error sending message to user {}: {}", userId, e.getMessage());
        }
    }
}