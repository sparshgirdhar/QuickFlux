CREATE TABLE reservations (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL,
    product_id      UUID NOT NULL REFERENCES products(id),
    quantity        INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL,

    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    expires_at      TIMESTAMP NOT NULL,
    confirmed_at    TIMESTAMP,

    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX idx_reservations_order_id ON reservations(order_id);
CREATE INDEX idx_reservations_product_id ON reservations(product_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at);