CREATE TABLE professional_verifications (
    professional_user_id UUID PRIMARY KEY REFERENCES professional_profiles(user_id) ON DELETE CASCADE,
    business_type VARCHAR(30) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    organization_number VARCHAR(32) NOT NULL,
    f_tax_confirmed BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    review_note VARCHAR(500),
    submitted_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT professional_verification_business_type_valid
        CHECK (business_type IN ('COMPANY', 'SOLE_TRADER')),
    CONSTRAINT professional_verification_status_valid
        CHECK (status IN ('NOT_STARTED', 'PENDING', 'VERIFIED', 'CHANGES_REQUIRED', 'EXPIRED')),
    CONSTRAINT professional_verification_country_code_valid
        CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_professional_verifications_status
    ON professional_verifications(status, submitted_at);
