package com.quickflux.orderservice.api;

import java.util.UUID;

public record CreateOrderResponse(UUID orderId, String message) {
}