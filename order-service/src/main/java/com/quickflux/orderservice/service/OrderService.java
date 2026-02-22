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
import org.springframework.transaction.annotation.Propagation;
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

    public UUID createOrder(CreateOrderRequest request) {
        log.info("Creating order for product {} x {}", request.getProductId(), request.getQuantity());

        // Step 1: Calculate total
        BigDecimal totalAmount = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // Step 2: Create order in CREATED state (in separate transaction)
        UUID orderId = createOrderInNewTransaction(request, totalAmount);

        // Step 3: Phase 1 - Parallel validation calls (REST)
        try {
            performPhase1Validation(orderId);
            publishOrderCreatedEvent(orderId);
            return orderId;

        } catch (Exception e) {
            log.error("Phase 1 failed for order {}: {}", orderId, e.getMessage());
            cancelOrderInNewTransaction(orderId);
            throw new OrderCreationFailedException("Order creation failed: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createOrderInNewTransaction(CreateOrderRequest request, BigDecimal totalAmount) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .build();

        orderRepository.save(order);
        log.info("Order {} created in CREATED state", order.getId());
        return order.getId();
    }

    private void performPhase1Validation(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        log.info("Starting Phase 1 validation for order {}", order.getId());

        CompletableFuture<UUID> reservationFuture = CompletableFuture.supplyAsync(
                () -> inventoryClient.reserveStock(
                        order.getId(),
                        order.getProductId(),
                        order.getQuantity()
                )
        );

        CompletableFuture<PaymentClient.PreAuthResult> preauthFuture = CompletableFuture.supplyAsync(
                () -> paymentClient.preAuthorizePayment(order.getId(), order.getTotalAmount())
        );

        try {
            CompletableFuture.allOf(reservationFuture, preauthFuture).join();

            UUID reservationId = reservationFuture.get();
            PaymentClient.PreAuthResult preauthResult = preauthFuture.get();

            // Both succeeded - update order to PENDING
            updateOrderToPending(order.getId(), reservationId, preauthResult.preauthId());

            log.info("Phase 1 complete for order {}. ReservationId: {}, PreauthId: {}",
                    order.getId(), reservationId, preauthResult.preauthId());

        } catch (Exception e) {
            cleanupPhase1PartialFailure(reservationFuture, preauthFuture, order.getId());
            throw new RuntimeException("Phase 1 validation failed", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOrderToPending(UUID orderId, UUID reservationId, UUID preauthId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.markAsPending(reservationId, preauthId);
        orderRepository.save(order);
    }

    private void publishOrderCreatedEvent(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

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

    // Extract Phase 1 logic
    private void performPhase1Validation(Order order) {
        log.info("Starting Phase 1 validation for order {}", order.getId());

        // CRITICAL: Make both calls concurrently
        CompletableFuture<UUID> reservationFuture = CompletableFuture.supplyAsync(
                () -> inventoryClient.reserveStock(
                        order.getId(),
                        order.getProductId(),
                        order.getQuantity()
                )
        );

        CompletableFuture<PaymentClient.PreAuthResult> preauthFuture = CompletableFuture.supplyAsync(
                () -> paymentClient.preAuthorizePayment(order.getId(), order.getTotalAmount())
        );

        // Wait for both to complete
        try {
            CompletableFuture.allOf(reservationFuture, preauthFuture).join();

            UUID reservationId = reservationFuture.get();
            PaymentClient.PreAuthResult preauthResult = preauthFuture.get();

            // Both succeeded - update order to PENDING
            order.markAsPending(reservationId, preauthResult.preauthId());
            orderRepository.save(order);

            log.info("Phase 1 complete for order {}. ReservationId: {}, PreauthId: {}",
                    order.getId(), reservationId, preauthResult.preauthId());

        } catch (Exception e) {
            cleanupPhase1PartialFailure(reservationFuture, preauthFuture, order.getId());
            throw new RuntimeException("Phase 1 validation failed", e);
        }
    }

    @Transactional
    public void forceCancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // Force cancel regardless of current status
        log.warn("Force cancelling order {} from status {}", orderId, order.getStatus());
        order.setStatus(OrderStatus.CANCELLED);  // Direct set, bypass state machine
        orderRepository.save(order);
    }

    private void cleanupPhase1PartialFailure(
            CompletableFuture<UUID> reservationFuture,
            CompletableFuture<PaymentClient.PreAuthResult> preauthFuture,
            UUID orderId) {

        // Check if reservation succeeded
        if (reservationFuture.isDone() && !reservationFuture.isCompletedExceptionally()) {
            try {
                UUID reservationId = reservationFuture.get();
                log.warn("Reservation {} succeeded but payment failed. Releasing stock...", reservationId);
                inventoryClient.releaseStock(orderId);
            } catch (Exception ex) {
                log.error("Failed to release stock during cleanup: {}", ex.getMessage());
            }
        }

        // Check if pre-auth succeeded
        if (preauthFuture.isDone() && !preauthFuture.isCompletedExceptionally()) {
            try {
                PaymentClient.PreAuthResult preauth = preauthFuture.get();
                log.warn("Pre-auth {} succeeded but reservation failed. Voiding payment...", preauth.preauthId());
                paymentClient.voidPreAuth(preauth.preauthId());
            } catch (Exception ex) {
                log.error("Failed to void pre-auth during cleanup: {}", ex.getMessage());
            }
        }
    }

    // Cancel in a separate transaction so it commits even if parent rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelOrderInNewTransaction(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.cancel();
        orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
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