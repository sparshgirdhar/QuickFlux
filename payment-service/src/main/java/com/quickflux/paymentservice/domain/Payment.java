package com.quickflux.paymentservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID orderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID preauthId;

    @Column(length = 100)
    private String gatewayReferenceId;

    @Column(length = 100)
    private String captureId;

    @Column(nullable = false)
    private Instant preauthAt;

    private Instant capturedAt;

    @Column(nullable = false, unique = true, length = 200)
    private String preauthIdempotencyKey;

    @Column(length = 200)
    private String captureIdempotencyKey;

    public void markAsCaptured(String captureId) {
        if (this.status != PaymentStatus.PRE_AUTHORIZED) {
            throw new IllegalStateException("Can only capture PRE_AUTHORIZED payments");
        }
        this.status = PaymentStatus.CAPTURED;
        this.captureId = captureId;
        this.capturedAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }
}