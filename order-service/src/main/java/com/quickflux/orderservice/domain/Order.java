package com.quickflux.orderservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(columnDefinition = "uuid")
    private UUID reservationId;

    @Column(columnDefinition = "uuid")
    private UUID paymentPreauthId;

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    // State transitions
    public void markAsPending(UUID reservationId, UUID paymentPreauthId) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("Can only mark CREATED orders as PENDING");
        }
        this.status = OrderStatus.PENDING;
        this.reservationId = reservationId;
        this.paymentPreauthId = paymentPreauthId;
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only confirm PENDING orders");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel CONFIRMED orders");
        }
        this.status = OrderStatus.CANCELLED;
    }
}