CREATE TABLE processed_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE UNIQUE INDEX idx_processed_events_event_id ON processed_events(event_id);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);