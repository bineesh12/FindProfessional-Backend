CREATE TABLE professional_subscription_entitlements (
    id UUID PRIMARY KEY,
    professional_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source VARCHAR(32) NOT NULL,
    external_reference VARCHAR(128),
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_subscription_period CHECK (expires_at > starts_at),
    CONSTRAINT uq_subscription_external_reference UNIQUE (source, external_reference)
);

CREATE INDEX idx_subscription_entitlements_professional_period
    ON professional_subscription_entitlements(professional_user_id, starts_at, expires_at);

CREATE TABLE professional_opportunity_views (
    id UUID PRIMARY KEY,
    professional_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_id UUID NOT NULL REFERENCES customer_requests(id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_professional_opportunity_view UNIQUE (professional_user_id, request_id)
);

CREATE INDEX idx_opportunity_views_professional_viewed_at
    ON professional_opportunity_views(professional_user_id, viewed_at);
