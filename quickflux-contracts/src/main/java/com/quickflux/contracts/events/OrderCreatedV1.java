package com.quickflux.contracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedV1(
        UUID eventId,
        String eventType,
        String version,
        UUID correlationId,
        Instant timestamp,
        String source,

        UUID orderId,
        UUID productId,
        int quantity,
        BigDecimal amount,
        UUID userId,
        UUID reservationId,
        UUID paymentPreAuthId
) implements DomainEvent {}