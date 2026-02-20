package com.quickflux.paymentservice.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FakeStripeGateway implements PaymentGateway {

    private final Map<String, String> idempotencyStore = new ConcurrentHashMap<>();
    private final Map<UUID, PreAuthResult> preauthStore = new ConcurrentHashMap<>();

    @Override
    public PreAuthResult preAuthorize(UUID orderId, BigDecimal amount, String idempotencyKey) {
        log.info("FakeStripe: Pre-authorizing ${} for order {}", amount, orderId);

        // Check idempotency
        String cached = idempotencyStore.get(idempotencyKey);
        if (cached != null) {
            log.info("FakeStripe: Returning cached pre-auth for key {}", idempotencyKey);
            UUID cachedId = UUID.fromString(cached);
            return preauthStore.get(cachedId);
        }

        // Simulate network delay
        simulateLatency(100, 300);

        // Simulate 5% failure rate
        if (Math.random() < 0.05) {
            log.error("FakeStripe: Pre-auth FAILED for order {}", orderId);
            throw new PaymentGatewayException("Simulated payment pre-auth failure");
        }

        UUID preauthId = UUID.randomUUID();
        String gatewayRef = "ch_fake_" + UUID.randomUUID().toString().substring(0, 8);

        PreAuthResult result = new PreAuthResult(preauthId, gatewayRef, "PRE_AUTHORIZED");
        preauthStore.put(preauthId, result);
        idempotencyStore.put(idempotencyKey, preauthId.toString());

        log.info("FakeStripe: Pre-auth SUCCESS {} for order {}", preauthId, orderId);
        return result;
    }

    @Override
    public CaptureResult capture(UUID preauthId, BigDecimal amount, String idempotencyKey) {
        log.info("FakeStripe: Capturing ${} for pre-auth {}", amount, preauthId);

        String cached = idempotencyStore.get(idempotencyKey);
        if (cached != null) {
            log.info("FakeStripe: Returning cached capture for key {}", idempotencyKey);
            return new CaptureResult(cached, "CAPTURED");
        }

        simulateLatency(100, 300);

        // Simulate 3% capture failure
        if (Math.random() < 0.03) {
            log.error("FakeStripe: Capture FAILED for pre-auth {}", preauthId);
            throw new PaymentGatewayException("Simulated capture failure");
        }

        String captureId = "cap_fake_" + UUID.randomUUID().toString().substring(0, 8);
        idempotencyStore.put(idempotencyKey, captureId);

        log.info("FakeStripe: Capture SUCCESS {}", captureId);
        return new CaptureResult(captureId, "CAPTURED");
    }

    @Override
    public void voidPreAuth(UUID preauthId, String idempotencyKey) {
        log.info("FakeStripe: Voiding pre-auth {}", preauthId);
        simulateLatency(50, 150);
        preauthStore.remove(preauthId);
        log.info("FakeStripe: Pre-auth {} voided", preauthId);
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            int delay = minMs + (int)(Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}