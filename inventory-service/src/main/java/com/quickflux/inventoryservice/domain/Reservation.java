package com.quickflux.inventoryservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID orderId;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant confirmedAt;

    @Version
    private Long version;

    public void confirm() {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Can only confirm RESERVED reservations");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void release() {
        if (this.status == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot release CONFIRMED reservations");
        }
        this.status = ReservationStatus.RELEASED;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}