ALTER TABLE users
    ADD COLUMN preferred_locale VARCHAR(10) NOT NULL DEFAULT 'en';

ALTER TABLE request_sessions
    ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en';

CREATE TABLE service_category_translations (
    category_id UUID NOT NULL REFERENCES service_categories(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (category_id, locale)
);

CREATE TABLE marketplace_service_translations (
    service_id UUID NOT NULL REFERENCES marketplace_services(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    short_description TEXT NOT NULL,
    search_keywords TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (service_id, locale)
);

CREATE TABLE service_question_translations (
    question_id UUID NOT NULL REFERENCES service_questions(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    prompt TEXT NOT NULL,
    helper_text TEXT,
    PRIMARY KEY (question_id, locale)
);

CREATE TABLE question_option_translations (
    option_id UUID NOT NULL REFERENCES question_options(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    label TEXT NOT NULL,
    description TEXT,
    PRIMARY KEY (option_id, locale)
);

CREATE INDEX idx_category_translations_locale ON service_category_translations(locale);
CREATE INDEX idx_service_translations_locale ON marketplace_service_translations(locale);
CREATE INDEX idx_question_translations_locale ON service_question_translations(locale);
CREATE INDEX idx_option_translations_locale ON question_option_translations(locale);
