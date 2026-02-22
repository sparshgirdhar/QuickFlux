package com.quickflux.orderservice.events;

import com.quickflux.orderservice.service.IdempotencyService;
import com.quickflux.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.quickflux.contracts.events.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    // Track which events we've received per order
    private final Map<UUID, EventTracker> eventTrackers = new ConcurrentHashMap<>();

    @KafkaListener(topics = "payment.captured", groupId = "order-service")
    public void handlePaymentCaptured(PaymentCapturedV1 event) {
        // Check idempotency
        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        log.info("Received PaymentCaptured event for order {}", event.orderId());

        UUID orderId = event.orderId();
        EventTracker tracker = eventTrackers.computeIfAbsent(orderId, k -> new EventTracker());
        tracker.paymentCaptured = true;

        checkAndConfirmOrder(orderId);

        // Mark as processed
        idempotencyService.markAsProcessed(event.eventId(), event.eventType());
    }

    @KafkaListener(topics = "stock.confirmed", groupId = "order-service")
    public void handleStockConfirmed(StockConfirmedV1 event) {
        // Check idempotency
        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        log.info("Received StockConfirmed event for order {}", event.orderId());

        UUID orderId = event.orderId();
        EventTracker tracker = eventTrackers.computeIfAbsent(orderId, k -> new EventTracker());
        tracker.stockConfirmed = true;

        checkAndConfirmOrder(orderId);

        // Mark as processed
        idempotencyService.markAsProcessed(event.eventId(), event.eventType());
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    public void handlePaymentFailed(PaymentFailedV1 event) {
        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        log.info("Received PaymentFailed event for order {}: {}",
                event.orderId(), event.reason());

        // Cancel the order (even if it was already confirmed)
        try {
            orderService.cancelOrder(event.orderId());
        } catch (IllegalStateException e) {
            // Order might be CONFIRMED already, force cancel it
            log.warn("Order {} already confirmed, forcing cancellation due to payment failure", event.orderId());
            orderService.forceCancelOrder(event.orderId());
        }

        idempotencyService.markAsProcessed(event.eventId(), event.eventType());
        eventTrackers.remove(event.orderId());
    }

    private void checkAndConfirmOrder(UUID orderId) {
        EventTracker tracker = eventTrackers.get(orderId);

        // Both events received?
        if (tracker != null && tracker.paymentCaptured && tracker.stockConfirmed) {
            log.info("Both payment and stock confirmed for order {}. Marking as CONFIRMED", orderId);

            orderService.confirmOrder(orderId);

            // Clean up tracker
            eventTrackers.remove(orderId);
        }
    }

    private static class EventTracker {
        boolean paymentCaptured = false;
        boolean stockConfirmed = false;
    }
}