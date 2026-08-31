CREATE TABLE professional_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_user_id UUID NOT NULL REFERENCES professional_profiles(user_id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES marketplace_services(id),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT professional_services_unique_service UNIQUE (professional_user_id, service_id),
    CONSTRAINT professional_services_display_order_non_negative CHECK (display_order >= 0)
);

CREATE UNIQUE INDEX idx_professional_services_one_primary
    ON professional_services(professional_user_id)
    WHERE is_primary = TRUE;

CREATE INDEX idx_professional_services_service
    ON professional_services(service_id, professional_user_id);

INSERT INTO professional_services (professional_user_id, service_id, is_primary, display_order)
SELECT user_id, primary_service_id, TRUE, 0
FROM professional_profiles;

CREATE TABLE portfolio_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_user_id UUID NOT NULL REFERENCES professional_profiles(user_id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES marketplace_services(id),
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT portfolio_projects_display_order_non_negative CHECK (display_order >= 0)
);

CREATE INDEX idx_portfolio_projects_professional
    ON portfolio_projects(professional_user_id, display_order, created_at);

CREATE TABLE portfolio_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES portfolio_projects(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT portfolio_images_size_positive CHECK (size_bytes > 0),
    CONSTRAINT portfolio_images_display_order_non_negative CHECK (display_order >= 0)
);

CREATE INDEX idx_portfolio_images_project
    ON portfolio_images(project_id, display_order, created_at);
