CREATE TABLE claim_attachment (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id       UUID         NOT NULL REFERENCES claim(id) ON DELETE CASCADE,
    object_key     VARCHAR(255) NOT NULL UNIQUE,
    original_name  VARCHAR(255) NOT NULL,
    content_type   VARCHAR(128) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    uploaded_by    VARCHAR(128) NOT NULL,
    uploaded_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_claim_attachment_claim_id    ON claim_attachment(claim_id);
CREATE INDEX idx_claim_attachment_uploaded_at ON claim_attachment(uploaded_at DESC);
