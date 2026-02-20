package com.quickflux.contracts.events;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();
    String eventType();
    String version();
    UUID correlationId();
    Instant timestamp();
    String source();
}
