package com.quickflux.contracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockConfirmedV1(
        UUID eventId,
        String eventType,
        String version,
        UUID correlationId,
        Instant timestamp,
        String source,

        UUID orderId,
        UUID userId
//        UUID productId,
//        int quantity
) implements DomainEvent {}
