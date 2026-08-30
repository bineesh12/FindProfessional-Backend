CREATE TABLE service_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES marketplace_services(id) ON DELETE CASCADE,
    phrase VARCHAR(160) NOT NULL,
    weight INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_service_alias_phrase UNIQUE (service_id, phrase),
    CONSTRAINT service_alias_weight_check CHECK (weight BETWEEN 1 AND 100)
);

CREATE INDEX idx_service_aliases_service_active
    ON service_aliases(service_id, active);

CREATE TABLE request_service_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES request_sessions(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES marketplace_services(id),
    score INTEGER NOT NULL,
    confidence VARCHAR(16) NOT NULL,
    source VARCHAR(24) NOT NULL,
    matched_phrase VARCHAR(160),
    suggestion_rank INTEGER NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_request_service_suggestion UNIQUE (session_id, service_id),
    CONSTRAINT request_service_score_check CHECK (score >= 0)
);

CREATE INDEX idx_request_service_suggestions_session_rank
    ON request_service_suggestions(session_id, suggestion_rank);

WITH aliases(service_code, phrase, weight) AS (
    VALUES
        ('GARDEN_MAINTENANCE', 'cut grass', 100),
        ('GARDEN_MAINTENANCE', 'garden grass', 100),
        ('GARDEN_MAINTENANCE', 'mow lawn', 100),
        ('GARDEN_MAINTENANCE', 'lawn mowing', 100),
        ('GARDEN_MAINTENANCE', 'garden maintenance', 95),
        ('GARDEN_MAINTENANCE', 'fix garden', 80),
        ('TREE_CARE', 'cut tree', 95),
        ('TREE_CARE', 'prune tree', 100),
        ('TREE_CARE', 'remove stump', 100),
        ('LANDSCAPING', 'landscape design', 100),
        ('LANDSCAPING', 'build patio', 90),
        ('MOVING_HELP', 'move house', 100),
        ('MOVING_HELP', 'move apartment', 100),
        ('MOVING_HELP', 'moving company', 95),
        ('PACKING_HELP', 'pack boxes', 100),
        ('PACKING_HELP', 'packing help', 95),
        ('JUNK_REMOVAL', 'remove junk', 100),
        ('LANGUAGE_LESSONS', 'learn swedish', 100),
        ('LANGUAGE_LESSONS', 'language lessons', 95),
        ('NETWORK_SETUP', 'fix wifi', 100),
        ('NETWORK_SETUP', 'wifi setup', 95),
        ('EVENT_CATERING', 'wedding catering', 100),
        ('PET_CARE', 'walk my dog', 100),
        ('HOME_CLEANING', 'clean my home', 100),
        ('PLUMBING', 'blocked drain', 100),
        ('ELECTRICAL', 'electrical socket', 100),
        ('APPLIANCE_REPAIR', 'washing machine', 100)
)
INSERT INTO service_aliases (service_id, phrase, weight)
SELECT service.id, alias.phrase, alias.weight
FROM aliases alias
JOIN marketplace_services service ON service.code = alias.service_code
ON CONFLICT (service_id, phrase) DO UPDATE SET
    weight = EXCLUDED.weight,
    active = TRUE;

UPDATE service_questions question
SET display_order = CASE question.question_key
    WHEN 'preferred_date' THEN 6
    WHEN 'service_location' THEN 7
    ELSE question.display_order
END,
updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'GARDEN_MAINTENANCE';

WITH garden_service AS (
    SELECT id FROM marketplace_services WHERE code = 'GARDEN_MAINTENANCE'
)
INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order
)
SELECT id, 'garden_work_type', 'What kind of garden work do you need?',
       'Choose the closest match. Add extra details in the request description.', 'SINGLE_CHOICE', 2
FROM garden_service
UNION ALL
SELECT id, 'garden_size', 'Approximately how large is the garden?',
       'An estimate in square metres is enough.', 'NUMBER', 3
FROM garden_service
UNION ALL
SELECT id, 'service_frequency', 'Is this a one-time or recurring service?',
       NULL, 'SINGLE_CHOICE', 5
FROM garden_service
ON CONFLICT (service_id, question_key) DO NOTHING;

WITH garden_service AS (
    SELECT id FROM marketplace_services WHERE code = 'GARDEN_MAINTENANCE'
)
INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order,
    condition_question_key, condition_value
)
SELECT id, 'waste_removal', 'Should the professional remove the garden waste?',
       'This question appears because garden cleanup was selected.', 'BOOLEAN', 4,
       'garden_work_type', 'GARDEN_CLEANUP'
FROM garden_service
ON CONFLICT (service_id, question_key) DO NOTHING;

INSERT INTO question_options (question_id, option_value, label, description, display_order)
SELECT question.id, option.value, option.label, option.description, option.display_order
FROM service_questions question
JOIN marketplace_services service ON service.id = question.service_id
CROSS JOIN (
    VALUES
        ('LAWN_MOWING', 'Lawn mowing', 'Cut and tidy grass areas.', 1),
        ('HEDGE_TRIMMING', 'Hedge trimming', 'Trim and shape hedges or shrubs.', 2),
        ('GARDEN_CLEANUP', 'Garden cleanup', 'Clear leaves, branches, or garden waste.', 3),
        ('MULTIPLE', 'Several tasks', 'The work includes more than one garden task.', 4)
) AS option(value, label, description, display_order)
WHERE service.code = 'GARDEN_MAINTENANCE'
  AND question.question_key = 'garden_work_type'
ON CONFLICT (question_id, option_value) DO NOTHING;

INSERT INTO question_options (question_id, option_value, label, description, display_order)
SELECT question.id, option.value, option.label, NULL, option.display_order
FROM service_questions question
JOIN marketplace_services service ON service.id = question.service_id
CROSS JOIN (
    VALUES ('ONE_TIME', 'One time', 1), ('RECURRING', 'Recurring', 2), ('UNSURE', 'Not sure', 3)
) AS option(value, label, display_order)
WHERE service.code = 'GARDEN_MAINTENANCE'
  AND question.question_key = 'service_frequency'
ON CONFLICT (question_id, option_value) DO NOTHING;

UPDATE service_questions question
SET display_order = CASE question.question_key
    WHEN 'preferred_date' THEN 4
    WHEN 'moving_from' THEN 5
    WHEN 'moving_to' THEN 6
    ELSE question.display_order
END,
updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'MOVING_HELP';

WITH moving_service AS (
    SELECT id FROM marketplace_services WHERE code = 'MOVING_HELP'
)
INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order,
    condition_question_key, condition_value
)
SELECT id, 'property_type', 'What type of property are you moving from?',
       NULL, 'SINGLE_CHOICE', 2, NULL, NULL
FROM moving_service
UNION ALL
SELECT id, 'pickup_elevator', 'Is there an elevator at the pickup property?',
       'This helps movers plan access and carrying time.', 'BOOLEAN', 3,
       'property_type', 'APARTMENT'
FROM moving_service
ON CONFLICT (service_id, question_key) DO NOTHING;

INSERT INTO question_options (question_id, option_value, label, description, display_order)
SELECT question.id, option.value, option.label, NULL, option.display_order
FROM service_questions question
JOIN marketplace_services service ON service.id = question.service_id
CROSS JOIN (
    VALUES
        ('APARTMENT', 'Apartment', 1),
        ('HOUSE', 'House', 2),
        ('OFFICE', 'Office', 3),
        ('OTHER', 'Other', 4)
) AS option(value, label, display_order)
WHERE service.code = 'MOVING_HELP'
  AND question.question_key = 'property_type'
ON CONFLICT (question_id, option_value) DO NOTHING;
