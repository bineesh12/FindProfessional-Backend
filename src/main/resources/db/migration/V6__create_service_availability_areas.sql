CREATE TABLE service_availability_areas (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES marketplace_services(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    service_radius_km DOUBLE PRECISION NOT NULL CHECK (service_radius_km > 0 AND service_radius_km <= 500),
    local_popularity_score INTEGER NOT NULL DEFAULT 0 CHECK (local_popularity_score >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_availability_active_location
    ON service_availability_areas(active, latitude, longitude);
CREATE INDEX idx_service_availability_service
    ON service_availability_areas(service_id);

-- Initial Stockholm coverage. Additional markets are data rows, not application code changes.
INSERT INTO service_availability_areas (
    id, service_id, latitude, longitude, service_radius_km, local_popularity_score
) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 59.3293, 18.0686, 60, 100),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 59.3293, 18.0686, 50, 90),
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 59.3293, 18.0686, 80, 80),
    ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', 59.3293, 18.0686, 40, 70);
