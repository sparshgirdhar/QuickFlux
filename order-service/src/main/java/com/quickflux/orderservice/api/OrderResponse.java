package com.quickflux.orderservice.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID userId,
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String status,
        UUID reservationId,
        UUID paymentPreauthId,
        Instant createdAt
) {}