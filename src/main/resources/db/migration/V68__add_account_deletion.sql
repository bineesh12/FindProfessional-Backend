ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE users DROP CONSTRAINT users_identity_check;

ALTER TABLE users
    ADD CONSTRAINT users_identity_check CHECK (
        deleted_at IS NOT NULL
        OR email IS NOT NULL
        OR phone_number IS NOT NULL
        OR google_subject IS NOT NULL
        OR firebase_uid IS NOT NULL
    );

CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;
