package com.quickflux.inventoryservice.service;

import com.quickflux.inventoryservice.domain.Product;
import com.quickflux.inventoryservice.domain.Reservation;
import com.quickflux.inventoryservice.domain.ReservationStatus;
import com.quickflux.inventoryservice.repository.ProductRepository;
import com.quickflux.inventoryservice.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    private static final int RESERVATION_TTL_MINUTES = 15;

    /**
     * CRITICAL: This method is atomic - all-or-nothing
     * Either stock is reserved or exception is thrown
     */
    @Transactional
    public UUID reserveStock(UUID orderId, UUID productId, int quantity) {
        log.info("Reserving stock for order {}: product {}, quantity {}",
                orderId, productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

        // This throws InsufficientStockException if stock is too low
        product.reduceStock(quantity);
        productRepository.save(product);  // Triggers optimistic lock check

        // Create reservation with TTL
        Instant now = Instant.now();
        Instant expiresAt = now.plus(RESERVATION_TTL_MINUTES, ChronoUnit.MINUTES);

        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        reservationRepository.save(reservation);

        log.info("Stock reserved successfully for order {}, reservation {}, expires at {}",
                orderId, reservation.getId(), expiresAt);

        return reservation.getId();
    }

    @Transactional
    public void confirmReservation(UUID orderId) {
        log.info("Confirming reservations for order {}", orderId);

        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            throw new ReservationNotFoundException("No reservations found for order: " + orderId);
        }

        reservations.forEach(Reservation::confirm);
        reservationRepository.saveAll(reservations);

        log.info("Confirmed {} reservation(s) for order {}", reservations.size(), orderId);
    }

    @Transactional
    public void releaseReservation(UUID orderId) {
        log.info("Releasing reservations for order {}", orderId);

        List<Reservation> reservations = reservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            log.warn("No reservations found to release for order {}", orderId);
            return;
        }

        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.RESERVED) {
                reservation.release();

                // Restore stock
                Product product = productRepository.findById(reservation.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException("Product not found: " + reservation.getProductId()));

                product.restoreStock(reservation.getQuantity());
                productRepository.save(product);

                log.info("Released reservation {} and restored {} units of product {}",
                        reservation.getId(), reservation.getQuantity(), reservation.getProductId());
            }
        }

        reservationRepository.saveAll(reservations);
    }
}