package com.quickflux.orderservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String INVENTORY_SERVICE_URL = "http://localhost:8083";

    public UUID reserveStock(UUID orderId, UUID productId, int quantity) {
        String url = INVENTORY_SERVICE_URL + "/api/inventory/reserve" +
                "?orderId=" + orderId +
                "&productId=" + productId +
                "&quantity=" + quantity;

        log.info("Calling Inventory Service: {}", url);

        ReserveResponse response = restTemplate.postForObject(url, null, ReserveResponse.class);
        return response.reservationId();
    }

    public record ReserveResponse(UUID reservationId) {}
}