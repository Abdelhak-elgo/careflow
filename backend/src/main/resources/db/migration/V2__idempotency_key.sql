CREATE TABLE idempotency_key (
    key         VARCHAR(128) PRIMARY KEY,
    claim_id    UUID         NOT NULL REFERENCES claim(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_idempotency_key_created_at ON idempotency_key(created_at);
