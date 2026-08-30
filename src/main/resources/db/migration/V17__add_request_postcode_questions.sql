-- Leave gaps between existing questions so a postcode can follow its
-- corresponding municipality without modifying an applied migration.
UPDATE service_questions
SET display_order = display_order * 2,
    updated_at = NOW();

INSERT INTO service_questions (
    service_id,
    question_key,
    prompt,
    helper_text,
    question_type,
    required,
    display_order,
    condition_question_key,
    condition_value
)
SELECT
    location.service_id,
    CASE location.question_key
        WHEN 'project_location' THEN 'project_postcode'
        WHEN 'moving_from' THEN 'pickup_postcode'
        WHEN 'moving_to' THEN 'destination_postcode'
        ELSE location.question_key || '_postcode'
    END,
    CASE location.question_key
        WHEN 'project_location' THEN 'What is the project postcode?'
        WHEN 'moving_from' THEN 'What is the pickup postcode?'
        WHEN 'moving_to' THEN 'What is the destination postcode?'
        ELSE 'What is the postcode for this location?'
    END,
    'Enter five digits, for example 418 33. Do not enter a street address.',
    'POSTCODE',
    location.required,
    location.display_order + 1,
    location.condition_question_key,
    location.condition_value
FROM service_questions location
WHERE location.question_type = 'LOCATION'
  AND location.active = TRUE
ON CONFLICT (service_id, question_key) DO NOTHING;
