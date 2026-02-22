package com.quickflux.orderservice.api;

import com.quickflux.orderservice.domain.Order;
import com.quickflux.orderservice.repository.OrderRepository;
import com.quickflux.orderservice.service.OrderNotFoundException;
import com.quickflux.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("Received create order request: {}", request);

        UUID orderId = orderService.createOrder(request);

        return ResponseEntity.accepted()
                .body(new CreateOrderResponse(orderId, "Order is being processed"));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        log.info("Received get order request for {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        OrderResponse response = new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus().toString(),
                order.getReservationId(),
                order.getPaymentPreauthId(),
                order.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }
}