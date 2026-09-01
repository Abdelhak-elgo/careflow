CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE claim (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id       VARCHAR(64)   NOT NULL,
    care_type        VARCHAR(32)   NOT NULL,
    amount           NUMERIC(19,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    care_date        DATE          NOT NULL,
    status           VARCHAR(16)   NOT NULL,
    decision_reason  TEXT,
    submitted_at     TIMESTAMPTZ   NOT NULL,
    decided_at       TIMESTAMPTZ
);

CREATE INDEX idx_claim_status       ON claim(status);
CREATE INDEX idx_claim_patient_id   ON claim(patient_id);
CREATE INDEX idx_claim_submitted_at ON claim(submitted_at);
