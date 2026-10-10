CREATE TABLE professional_reviews (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE REFERENCES customer_requests(id) ON DELETE CASCADE,
    offer_id UUID NOT NULL UNIQUE REFERENCES professional_offers(id) ON DELETE CASCADE,
    customer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT professional_reviews_rating_valid CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_professional_reviews_professional_created
    ON professional_reviews(professional_user_id, created_at DESC, id DESC);
