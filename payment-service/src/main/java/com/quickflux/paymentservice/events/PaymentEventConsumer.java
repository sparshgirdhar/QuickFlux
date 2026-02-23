package com.quickflux.paymentservice.events;

import com.quickflux.paymentservice.gateway.PaymentGateway.CaptureResult;
import com.quickflux.paymentservice.service.IdempotencyService;
import com.quickflux.paymentservice.service.PaymentService;
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
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = "order.created", groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedV1 event) {
        // Check idempotency
        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        log.info("Received OrderCreated event {}", event.eventId());

        UUID orderId = event.orderId();
        UUID preauthId = event.paymentPreAuthId();

        try {
            // Phase 2: Capture the pre-authorized payment
            CaptureResult result = paymentService.capturePayment(preauthId, event.amount());

            log.info("Payment captured successfully for order {}: {}", orderId, result);

            // Publish PaymentCaptured event
            PaymentCapturedV1 capturedEvent = new PaymentCapturedV1(
                    UUID.randomUUID(),
                    "PaymentCaptured",
                    "v1",
                    event.correlationId(),
                    Instant.now(),
                    "payment-service",
                    orderId,
                    event.amount(),
                    event.userId()
            );

            eventPublisher.publish("payment.captured", orderId.toString(), capturedEvent);

        } catch (Exception e) {
            log.error("Payment capture failed for order {}: {}", orderId, e.getMessage());

            // IMPORTANT: Mark payment as FAILED in the database
            paymentService.markPaymentAsFailed(preauthId);

            // Publish compensation event
            PaymentFailedV1 failedEvent = new PaymentFailedV1(
                    UUID.randomUUID(),
                    "PaymentFailed",
                    "v1",
                    event.correlationId(),
                    Instant.now(),
                    "payment-service",
                    orderId,
                    "CAPTURE_FAILED: " + e.getMessage(),
                    event.reservationId(),
                    event.userId()
            );

            eventPublisher.publish("payment.failed", orderId.toString(), failedEvent);
        }

        // Mark as processed
        idempotencyService.markAsProcessed(event.eventId(), event.eventType());
    }
}