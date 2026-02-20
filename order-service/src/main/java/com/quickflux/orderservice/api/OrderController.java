package com.quickflux.orderservice.api;

import com.quickflux.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("Received create order request: {}", request);

        UUID orderId = orderService.createOrder(request);

        return ResponseEntity.accepted()
                .body(new CreateOrderResponse(orderId, "Order is being processed"));
    }
}