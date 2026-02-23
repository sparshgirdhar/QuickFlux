package com.quickflux.inventoryservice.events;

import com.quickflux.inventoryservice.service.IdempotencyService;
import com.quickflux.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;
import com.quickflux.contracts.events.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;
    private final EventPublisher eventPublisher;

//    @KafkaListener(topics = "order.created", groupId = "inventory-service")
//    public void handleOrderCreated(OrderCreatedV1 event) {
//        // Check idempotency
//        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
//            log.info("Event {} already processed, skipping", event.eventId());
//            return;
//        }
//
//        log.info("Received OrderCreated event {}", event.eventId());
//
//        UUID orderId = event.orderId();
//
//        try {
//            // Phase 2: Confirm the reservation from Phase 1
//            inventoryService.confirmReservation(orderId);
//
//            log.info("Stock confirmed successfully for order {}", orderId);
//
//            // Publish StockConfirmed event
//            StockConfirmedV1 confirmedEvent = new StockConfirmedV1(
//                    UUID.randomUUID(),
//                    "StockConfirmed",
//                    "v1",
//                    event.correlationId(),
//                    Instant.now(),
//                    "inventory-service",
//                    orderId,
//                    event.productId(),
//                    event.quantity()
//            );
//
//            eventPublisher.publish("stock.confirmed", orderId.toString(), confirmedEvent);
//
//        } catch (Exception e) {
//            log.error("Stock confirmation failed for order {}: {}", orderId, e.getMessage());
//            // In a real system, publish StockConfirmationFailed event here
//            // For Week 1, we skip this - it's handled by PaymentFailed compensation
//        }
//
//        // Mark as processed
//        idempotencyService.markAsProcessed(event.eventId(), event.eventType());
//    }

    @KafkaListener(topics = "payment.captured", groupId = "inventory-service")
    public void handlePaymentCaptured(PaymentCapturedV1 event) {
        // Check idempotency
        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        log.info("Received PaymentCaptured event for order {}", event.orderId());

        UUID orderId = event.orderId();

        try {
            // Payment succeeded - NOW confirm the reservation
            inventoryService.confirmReservation(orderId);

            log.info("Stock confirmed successfully for order {}", orderId);

            // Publish StockConfirmed event
            StockConfirmedV1 confirmedEvent = new StockConfirmedV1(
                    UUID.randomUUID(),
                    "StockConfirmed",
                    "v1",
                    event.correlationId(),
                    Instant.now(),
                    "inventory-service",
                    orderId,
                    event.userId()
//                    null,  // We don't have productId in PaymentCaptured, that's OK
//                    0      // We don't have quantity in PaymentCaptured, that's OK
            );

            eventPublisher.publish("stock.confirmed", orderId.toString(), confirmedEvent);

        } catch (Exception e) {
            log.error("Stock confirmation failed for order {}: {}", orderId, e.getMessage());
            // This shouldn't happen since reservation was already made in Phase 1
        }

        // Mark as processed
        idempotencyService.markAsProcessed(event.eventId(), event.eventType());
    }

    @KafkaListener(topics = "payment.failed", groupId = "inventory-service")
    public void handlePaymentFailed(PaymentFailedV1 event) {
        // Check idempotency
        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        log.info("Received PaymentFailed event for order {}: {}",
                event.orderId(), event.reason());

        // Compensation: Release the reservation and restore stock
        inventoryService.releaseReservation(event.orderId());

        // Mark as processed
        idempotencyService.markAsProcessed(event.eventId(), event.eventType());
    }
}