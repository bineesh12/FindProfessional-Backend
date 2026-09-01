ALTER TABLE professional_profiles
    ADD COLUMN service_postal_code VARCHAR(20),
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN service_radius_km DOUBLE PRECISION NOT NULL DEFAULT 50;

ALTER TABLE professional_profiles
    ADD CONSTRAINT professional_profile_coordinates_complete CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),
    ADD CONSTRAINT professional_profile_latitude_valid CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT professional_profile_longitude_valid CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT professional_profile_radius_valid CHECK (service_radius_km BETWEEN 1 AND 500);

CREATE TABLE professional_opportunity_declines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_id UUID NOT NULL REFERENCES customer_requests(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT professional_opportunity_declines_unique UNIQUE (professional_user_id, request_id)
);

CREATE INDEX idx_professional_opportunity_declines_professional
    ON professional_opportunity_declines(professional_user_id, request_id);

CREATE TABLE professional_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_id UUID NOT NULL REFERENCES customer_requests(id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    estimated_days INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT professional_offers_unique UNIQUE (professional_user_id, request_id),
    CONSTRAINT professional_offers_amount_positive CHECK (amount > 0),
    CONSTRAINT professional_offers_estimated_days_valid CHECK (estimated_days BETWEEN 1 AND 3650),
    CONSTRAINT professional_offers_status_valid CHECK (status IN ('SUBMITTED', 'WITHDRAWN'))
);

CREATE INDEX idx_professional_offers_professional
    ON professional_offers(professional_user_id, status, updated_at DESC);

CREATE INDEX idx_professional_offers_request
    ON professional_offers(request_id, status, updated_at DESC);
