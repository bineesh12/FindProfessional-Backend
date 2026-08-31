INSERT INTO marketplace_services (
    category_id, code, name, short_description, icon_key, search_keywords, popular
)
SELECT
    category.id,
    'HOUSE_CONSTRUCTION',
    'House construction',
    'Plan and build a new home',
    'construction',
    'build house home new construction turnkey contractor land architect',
    FALSE
FROM service_categories category
WHERE category.code = 'HOME'
ON CONFLICT (code) DO NOTHING;

CREATE TABLE service_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES marketplace_services(id) ON DELETE CASCADE,
    question_key VARCHAR(96) NOT NULL,
    prompt VARCHAR(320) NOT NULL,
    helper_text VARCHAR(500),
    question_type VARCHAR(32) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL,
    condition_question_key VARCHAR(96),
    condition_value VARCHAR(240),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_service_question_key UNIQUE (service_id, question_key),
    CONSTRAINT service_question_condition_check CHECK (
        (condition_question_key IS NULL AND condition_value IS NULL) OR
        (condition_question_key IS NOT NULL AND condition_value IS NOT NULL)
    )
);

CREATE TABLE question_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES service_questions(id) ON DELETE CASCADE,
    option_value VARCHAR(96) NOT NULL,
    label VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    display_order INTEGER NOT NULL,
    CONSTRAINT uq_question_option_value UNIQUE (question_id, option_value)
);

CREATE INDEX idx_service_questions_service_order
    ON service_questions(service_id, active, display_order);
CREATE INDEX idx_question_options_question_order
    ON question_options(question_id, display_order);

CREATE TABLE request_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES service_categories(id),
    service_id UUID REFERENCES marketplace_services(id),
    status VARCHAR(32) NOT NULL,
    initial_description TEXT,
    current_question_id UUID REFERENCES service_questions(id),
    next_message_sequence INTEGER NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE request_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES request_sessions(id) ON DELETE CASCADE,
    sender VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_request_message_sequence UNIQUE (session_id, sequence_number)
);

CREATE TABLE request_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES request_sessions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES service_questions(id),
    answer_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_request_answer UNIQUE (session_id, question_id)
);

CREATE TABLE customer_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL UNIQUE REFERENCES request_sessions(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES service_categories(id),
    service_id UUID NOT NULL REFERENCES marketplace_services(id),
    status VARCHAR(32) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_request_sessions_customer_updated
    ON request_sessions(customer_id, updated_at DESC);
CREATE INDEX idx_request_messages_session_sequence
    ON request_messages(session_id, sequence_number);
CREATE INDEX idx_request_answers_session
    ON request_answers(session_id);
CREATE INDEX idx_customer_requests_customer_created
    ON customer_requests(customer_id, created_at DESC);

WITH house_service AS (
    SELECT id FROM marketplace_services WHERE code = 'HOUSE_CONSTRUCTION'
)
INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order,
    condition_question_key, condition_value
)
SELECT id, 'delivery_model',
       'Do you want one company to manage the complete construction?',
       'This is often called a "totalentreprenad" (turnkey contract) and simplifies the process.',
       'SINGLE_CHOICE', 1, NULL, NULL
FROM house_service
UNION ALL
SELECT id, 'land_owned', 'Do you already own the land?',
       'This helps professionals understand how far the project has progressed.',
       'BOOLEAN', 2, NULL, NULL
FROM house_service
UNION ALL
SELECT id, 'project_size', 'Approximately how large should the house be?',
       'Enter the planned living area in square metres.',
       'NUMBER', 3, NULL, NULL
FROM house_service
UNION ALL
SELECT id, 'project_location', 'Where will the house be built?',
       'Enter the city or municipality.',
       'LOCATION', 4, NULL, NULL
FROM house_service
UNION ALL
SELECT id, 'budget', 'What budget are you planning for the construction?',
       'Enter an approximate amount. You can refine it later.',
       'MONEY', 5, NULL, NULL
FROM house_service
UNION ALL
SELECT id, 'preferred_start_date', 'When would you like construction to begin?',
       'An approximate date is enough.',
       'DATE', 6, NULL, NULL
FROM house_service;

INSERT INTO question_options (question_id, option_value, label, description, display_order)
SELECT question.id, option.option_value, option.label, option.description, option.display_order
FROM service_questions question
CROSS JOIN (
    VALUES
        ('ONE_COMPANY', 'Yes, one company',
         'I prefer a single point of contact to handle everything from start to finish.', 1),
        ('SEPARATE', 'Hire separately',
         'I want to hire an architect, builders, and specialists individually.', 2),
        ('UNSURE', 'I''m not sure',
         'Help me decide what is best for my situation.', 3)
) AS option(option_value, label, description, display_order)
WHERE question.question_key = 'delivery_model';
