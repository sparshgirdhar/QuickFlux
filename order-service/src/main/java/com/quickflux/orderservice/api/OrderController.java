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

import java.util.List;
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
            @RequestHeader("X-User-Id") UUID userId,  // From API Gateway
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("Received create order request from user: {}", userId);

        UUID orderId = orderService.createOrder(request, userId);

        return ResponseEntity.accepted()
                .body(new CreateOrderResponse(orderId, "Order is being processed"));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId) {

        log.info("Received get order request for {} from user {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // Security: Verify order belongs to requesting user
        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Order not found: " + orderId);
        }

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

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Received get all orders request from user {}", userId);

        List<Order> orders = orderRepository.findByUserId(userId);

        List<OrderResponse> responses = orders.stream()
                .map(order -> new OrderResponse(
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
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }
}