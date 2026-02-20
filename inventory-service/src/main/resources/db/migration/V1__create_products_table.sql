CREATE TABLE products (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    sku             VARCHAR(50) UNIQUE NOT NULL,
    stock_level     INTEGER NOT NULL,

    version         BIGINT NOT NULL DEFAULT 0,

    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    CONSTRAINT chk_stock_level CHECK (stock_level >= 0)
);

CREATE UNIQUE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_stock_level ON products(stock_level);