CREATE TABLE user_blocks (
    id UUID PRIMARY KEY,
    blocker_user_id UUID NOT NULL REFERENCES users(id),
    blocked_user_id UUID NOT NULL REFERENCES users(id),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT user_blocks_different_users CHECK (blocker_user_id <> blocked_user_id),
    CONSTRAINT user_blocks_unique_pair UNIQUE (blocker_user_id, blocked_user_id)
);

CREATE INDEX user_blocks_blocked_user_idx ON user_blocks(blocked_user_id);

CREATE TABLE user_reports (
    id UUID PRIMARY KEY,
    reporter_user_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID NOT NULL REFERENCES users(id),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    reason VARCHAR(32) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT user_reports_different_users CHECK (reporter_user_id <> reported_user_id)
);

CREATE INDEX user_reports_status_created_idx ON user_reports(status, created_at);
CREATE INDEX user_reports_reported_user_idx ON user_reports(reported_user_id);
