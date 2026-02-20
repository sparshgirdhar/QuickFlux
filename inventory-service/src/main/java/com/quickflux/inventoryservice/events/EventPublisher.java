package com.quickflux.inventoryservice.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.quickflux.contracts.events.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, DomainEvent event) {
        log.info("Publishing event {} to topic {} with key {}",
                event.eventType(), topic, key);

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {}: {}", event.eventId(), ex.getMessage());
                    } else {
                        log.info("Event {} published successfully to partition {}",
                                event.eventId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}