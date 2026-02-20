CREATE TABLE payments (
    id                      UUID PRIMARY KEY,
    order_id                UUID NOT NULL,
    amount                  NUMERIC(10, 2) NOT NULL,
    status                  VARCHAR(20) NOT NULL,

    preauth_id              UUID NOT NULL,
    gateway_reference_id    VARCHAR(100),
    capture_id              VARCHAR(100),

    preauth_at              TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    captured_at             TIMESTAMP,

    preauth_idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    capture_idempotency_key VARCHAR(200),

    CONSTRAINT chk_payment_status CHECK (status IN ('PRE_AUTHORIZED', 'CAPTURED', 'VOIDED', 'FAILED')),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE UNIQUE INDEX idx_payments_preauth_idempotency ON payments(preauth_idempotency_key);