package com.quickflux.contracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedV1(
        UUID eventId,
        String eventType,
        String version,
        UUID correlationId,
        Instant timestamp,
        String source,

        UUID orderId,
        String reason,
        UUID reservationId  // NEW - tells Inventory which reservation to release
) implements DomainEvent {}
