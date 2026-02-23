package com.quickflux.contracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCapturedV1(
        UUID eventId,
        String eventType,
        String version,
        UUID correlationId,
        Instant timestamp,
        String source,

        UUID orderId,
        BigDecimal amount,
        UUID userId
) implements DomainEvent {}