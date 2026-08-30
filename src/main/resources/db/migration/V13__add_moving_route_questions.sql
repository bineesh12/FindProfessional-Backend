WITH routed_services AS (
    SELECT id
    FROM marketplace_services
    WHERE code IN ('MOVING_HELP', 'PACKING_HELP')
)
UPDATE service_questions question
SET question_key = 'moving_from',
    prompt = 'Which municipality or city are you moving from?',
    helper_text = 'Do not enter a street address. Share the exact pickup address after hiring.',
    updated_at = NOW()
FROM routed_services service
WHERE question.service_id = service.id
  AND question.question_key = 'service_location';

INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order
)
SELECT service.id,
       'moving_to',
       'Which municipality or city are you moving to?',
       'Do not enter a street address. Share the exact destination address after hiring.',
       'LOCATION',
       4
FROM marketplace_services service
WHERE service.code IN ('MOVING_HELP', 'PACKING_HELP')
ON CONFLICT (service_id, question_key) DO NOTHING;

UPDATE service_questions question
SET prompt = 'Which municipality or city should the items be collected from?',
    helper_text = 'Do not enter a street address. Share the exact pickup address after hiring.',
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'JUNK_REMOVAL'
  AND question.question_key = 'service_location';

UPDATE service_questions question
SET prompt = 'Which municipality or city is the furniture in?',
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'FURNITURE_ASSEMBLY'
  AND question.question_key = 'service_location';
