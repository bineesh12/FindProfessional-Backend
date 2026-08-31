CREATE TABLE postcode_coordinates (
    id UUID PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    postal_code VARCHAR(16) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    source VARCHAR(32) NOT NULL,
    resolved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_postcode_coordinate UNIQUE (country_code, postal_code)
);

CREATE TABLE request_locations (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES customer_requests(id) ON DELETE CASCADE,
    location_kind VARCHAR(24) NOT NULL,
    municipality VARCHAR(160) NOT NULL,
    postal_code VARCHAR(16) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_request_location_kind UNIQUE (request_id, location_kind)
);

CREATE INDEX idx_request_locations_coordinates
    ON request_locations(latitude, longitude);
CREATE INDEX idx_request_locations_postcode
    ON request_locations(postal_code);
