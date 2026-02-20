CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    product_id          UUID NOT NULL,
    quantity            INTEGER NOT NULL,
    unit_price          NUMERIC(10, 2) NOT NULL,
    total_amount        NUMERIC(10, 2) NOT NULL,
    status              VARCHAR(20) NOT NULL,

    reservation_id      UUID,
    payment_preauth_id  UUID,

    created_at          TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    version             BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_total_amount CHECK (total_amount > 0),
    CONSTRAINT chk_status CHECK (status IN ('CREATED', 'PENDING', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);