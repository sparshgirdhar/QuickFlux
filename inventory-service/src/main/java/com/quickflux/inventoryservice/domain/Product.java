package com.quickflux.inventoryservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false)
    private Integer stockLevel;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Atomically check and reduce stock
     * Throws exception if insufficient stock
     */
    public void reduceStock(Integer quantity) {
        if (this.stockLevel < quantity) {
            throw new InsufficientStockException(
                    "Product " + id + " has only " + stockLevel + " units, requested " + quantity
            );
        }
        this.stockLevel -= quantity;
        this.updatedAt = Instant.now();
    }

    public void restoreStock(Integer quantity) {
        this.stockLevel += quantity;
        this.updatedAt = Instant.now();
    }
}