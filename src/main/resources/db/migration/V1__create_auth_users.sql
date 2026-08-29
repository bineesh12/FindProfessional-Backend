CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) UNIQUE,
    phone_number VARCHAR(32) UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(160) NOT NULL,
    google_subject VARCHAR(255) UNIQUE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT users_identity_check CHECK (
        email IS NOT NULL OR phone_number IS NOT NULL OR google_subject IS NOT NULL
    )
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE phone_verifications (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(32) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_phone_verifications_phone_created_at
    ON phone_verifications(phone_number, created_at DESC);
