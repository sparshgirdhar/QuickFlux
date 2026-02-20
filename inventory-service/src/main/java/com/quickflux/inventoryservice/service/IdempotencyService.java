package com.quickflux.inventoryservice.service;

import com.quickflux.inventoryservice.domain.ProcessedEvent;
import com.quickflux.inventoryservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public void markAsProcessed(UUID eventId, String eventType) {
        ProcessedEvent processed = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(Instant.now())
                .build();

        processedEventRepository.save(processed);
        log.debug("Marked event {} as processed", eventId);
    }
}