package com.quickflux.inventoryservice.api;

import com.quickflux.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    public ResponseEntity<ReserveStockResponse> reserveStock(
            @RequestParam UUID orderId,
            @RequestParam UUID productId,
            @RequestParam int quantity) {

        log.info("Received reserve stock request for order {}, product {}, quantity {}",
                orderId, productId, quantity);

        UUID reservationId = inventoryService.reserveStock(orderId, productId, quantity);

        return ResponseEntity.ok(new ReserveStockResponse(reservationId));
    }

    public record ReserveStockResponse(UUID reservationId) {}
}