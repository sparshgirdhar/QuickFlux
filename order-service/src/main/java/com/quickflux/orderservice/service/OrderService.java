package com.quickflux.orderservice.service;

import com.quickflux.orderservice.api.CreateOrderRequest;
import com.quickflux.orderservice.client.InventoryClient;
import com.quickflux.orderservice.client.PaymentClient;
import com.quickflux.orderservice.domain.Order;
import com.quickflux.orderservice.domain.OrderStatus;
import com.quickflux.orderservice.events.EventPublisher;
import com.quickflux.contracts.events.OrderCreatedV1;
import com.quickflux.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final EventPublisher eventPublisher;

    // Hardcoded test user for Week 1
    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Transactional
    public UUID createOrder(CreateOrderRequest request) {
        log.info("Creating order for product {} x {}", request.getProductId(), request.getQuantity());

        // Step 1: Calculate total
        BigDecimal totalAmount = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // Step 2: Create order in CREATED state
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)  // Hardcoded for Week 1
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .build();

        orderRepository.save(order);
        log.info("Order {} created in CREATED state", order.getId());

        // Step 3: Phase 1 - Parallel validation calls (REST)
        try {
            log.info("Starting Phase 1 validation for order {}", order.getId());

            // CRITICAL: Make both calls concurrently
            CompletableFuture<UUID> reservationFuture = CompletableFuture.supplyAsync(
                    () -> inventoryClient.reserveStock(
                            order.getId(),
                            request.getProductId(),
                            request.getQuantity()
                    )
            );

            CompletableFuture<PaymentClient.PreAuthResult> preauthFuture = CompletableFuture.supplyAsync(
                    () -> paymentClient.preAuthorizePayment(order.getId(), totalAmount)
            );

            // Wait for both to complete
            CompletableFuture.allOf(reservationFuture, preauthFuture).join();

            UUID reservationId = reservationFuture.get();
            PaymentClient.PreAuthResult preauthResult = preauthFuture.get();

            // Both succeeded - update order to PENDING
            order.markAsPending(reservationId, preauthResult.preauthId());
            orderRepository.save(order);

            log.info("Phase 1 complete for order {}. ReservationId: {}, PreauthId: {}",
                    order.getId(), reservationId, preauthResult.preauthId());

            // Step 4: Phase 2 - Publish OrderCreated event
            publishOrderCreatedEvent(order);

            return order.getId();

        } catch (Exception e) {
            log.error("Phase 1 failed for order {}: {}", order.getId(), e.getMessage());
            order.cancel();
            orderRepository.save(order);
            throw new OrderCreationFailedException("Order creation failed: " + e.getMessage(), e);
        }
    }

    private void publishOrderCreatedEvent(Order order) {
        UUID correlationId = UUID.randomUUID();

        OrderCreatedV1 event = new OrderCreatedV1(
                UUID.randomUUID(),
                "OrderCreated",
                "v1",
                correlationId,
                Instant.now(),
                "order-service",
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getUserId(),
                order.getReservationId(),
                order.getPaymentPreauthId()
        );

        eventPublisher.publish("order.created", order.getId().toString(), event);
        log.info("OrderCreated event published for order {}", order.getId());
    }

    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.confirm();
        orderRepository.save(order);
        log.info("Order {} confirmed", orderId);
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.cancel();
        orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
    }
}