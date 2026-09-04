ALTER TABLE idempotency_key
    DROP CONSTRAINT IF EXISTS idempotency_key_claim_id_fkey;

ALTER TABLE idempotency_key
    RENAME COLUMN claim_id TO resource_id;

ALTER TABLE idempotency_key
    ADD COLUMN resource_type VARCHAR(32) NOT NULL DEFAULT 'CLAIM';

ALTER TABLE idempotency_key
    ALTER COLUMN resource_type DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_idempotency_key_resource
    ON idempotency_key(resource_type, resource_id);
