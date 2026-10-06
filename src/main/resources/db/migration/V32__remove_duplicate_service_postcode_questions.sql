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
    canonical.id,
    answer.answer_value,
    answer.created_at,
    NOW()
FROM request_answers answer
JOIN service_questions legacy ON legacy.id = answer.question_id
JOIN service_questions canonical
    ON canonical.service_id = legacy.service_id
    AND canonical.question_key IN ('service_postcode', 'pickup_postcode')
    AND canonical.active = TRUE
WHERE legacy.question_key = 'service_location_postcode'
ON CONFLICT (session_id, question_id) DO NOTHING;

UPDATE service_questions legacy
SET active = FALSE,
    updated_at = NOW()
WHERE legacy.question_key = 'service_location_postcode'
  AND legacy.active = TRUE
  AND EXISTS (
      SELECT 1
      FROM service_questions canonical
      WHERE canonical.service_id = legacy.service_id
        AND canonical.question_key IN ('service_postcode', 'pickup_postcode')
        AND canonical.active = TRUE
  );
