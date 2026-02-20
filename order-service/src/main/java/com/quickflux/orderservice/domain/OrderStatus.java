package com.quickflux.orderservice.domain;

public enum OrderStatus {
    CREATED,    // Initial state
    PENDING,    // Phase 1 validation passed
    CONFIRMED,  // Phase 2 complete
    CANCELLED   // Failed or cancelled
}
