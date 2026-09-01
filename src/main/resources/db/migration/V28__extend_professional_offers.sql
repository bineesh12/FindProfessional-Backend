ALTER TABLE professional_offers
    ALTER COLUMN amount DROP NOT NULL,
    ALTER COLUMN message DROP NOT NULL,
    ALTER COLUMN estimated_days DROP NOT NULL,
    ADD COLUMN available_start_date DATE,
    ADD COLUMN scope_included VARCHAR(2000),
    ADD COLUMN scope_excluded VARCHAR(2000);

ALTER TABLE professional_offers DROP CONSTRAINT professional_offers_status_valid;
ALTER TABLE professional_offers
    ADD CONSTRAINT professional_offers_status_valid CHECK (status IN ('DRAFT', 'SUBMITTED', 'WITHDRAWN'));

CREATE TABLE professional_offer_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_id UUID NOT NULL REFERENCES professional_offers(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT professional_offer_attachment_size_valid CHECK (size_bytes BETWEEN 1 AND 10485760)
);

CREATE INDEX idx_professional_offer_attachments_offer
    ON professional_offer_attachments(offer_id, created_at);
