CREATE TABLE audit_log (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at  TIMESTAMPTZ  NOT NULL,
    actor        VARCHAR(128) NOT NULL,
    action       VARCHAR(64)  NOT NULL,
    entity_type  VARCHAR(32)  NOT NULL,
    entity_id    VARCHAR(64),
    details      JSONB
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log(occurred_at DESC);
CREATE INDEX idx_audit_log_entity      ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_actor       ON audit_log(actor);
