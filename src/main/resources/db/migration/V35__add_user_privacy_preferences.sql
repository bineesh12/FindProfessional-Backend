ALTER TABLE users
    ADD COLUMN analytics_consent_granted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN analytics_consent_updated_at TIMESTAMPTZ;
