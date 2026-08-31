CREATE TABLE professional_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    business_name VARCHAR(120) NOT NULL,
    primary_service_id UUID NOT NULL REFERENCES marketplace_services(id),
    service_area VARCHAR(120) NOT NULL,
    experience_years INTEGER NOT NULL,
    contact_email VARCHAR(254) NOT NULL,
    about VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT professional_profiles_experience_range
        CHECK (experience_years BETWEEN 0 AND 80)
);

CREATE INDEX idx_professional_profiles_primary_service
    ON professional_profiles(primary_service_id);
