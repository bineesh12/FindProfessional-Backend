CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES customer_requests(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    professional_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_conversation_request_professional UNIQUE (request_id, professional_id)
);

CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_conversations_customer_updated
    ON conversations(customer_id, updated_at DESC);

CREATE INDEX idx_conversations_professional_updated
    ON conversations(professional_id, updated_at DESC);

CREATE INDEX idx_conversation_messages_conversation_created
    ON conversation_messages(conversation_id, created_at, id);

CREATE INDEX idx_conversation_messages_unread
    ON conversation_messages(conversation_id, sender_id, read_at)
    WHERE read_at IS NULL;
