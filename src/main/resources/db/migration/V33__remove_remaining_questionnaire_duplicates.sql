UPDATE service_questions question
SET prompt = 'What type of event is it?',
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code IN ('DJ_LIVE_MUSIC', 'EVENT_CATERING', 'EVENT_PHOTOGRAPHY')
  AND question.question_key = 'event_details'
  AND question.active = TRUE;

INSERT INTO request_answers (
    id,
    session_id,
    question_id,
    answer_value,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    answer.session_id,
    event_date.id,
    answer.answer_value,
    answer.created_at,
    NOW()
FROM request_answers answer
JOIN service_questions preferred_date ON preferred_date.id = answer.question_id
JOIN marketplace_services service
    ON service.id = preferred_date.service_id
    AND service.code = 'EVENT_PLANNING'
JOIN service_questions event_date
    ON event_date.service_id = service.id
    AND event_date.question_key = 'event_date'
    AND event_date.active = TRUE
WHERE preferred_date.question_key = 'preferred_date'
ON CONFLICT (session_id, question_id) DO NOTHING;

UPDATE service_questions question
SET active = FALSE,
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'EVENT_PLANNING'
  AND question.question_key = 'preferred_date'
  AND question.active = TRUE;

ALTER TABLE service_questions
    ADD CONSTRAINT chk_no_active_legacy_service_location_postcode
    CHECK (question_key <> 'service_location_postcode' OR active = FALSE);

CREATE UNIQUE INDEX uq_service_questions_active_type_prompt
    ON service_questions (
        service_id,
        question_type,
        LOWER(REGEXP_REPLACE(TRIM(prompt), '\s+', ' ', 'g'))
    )
    WHERE active = TRUE;
