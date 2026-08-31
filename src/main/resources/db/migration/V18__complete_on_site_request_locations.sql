WITH services_missing_location AS (
    SELECT
        service.id AS service_id,
        category.code AS category_code,
        COALESCE(MAX(question.display_order), 0) AS last_display_order
    FROM marketplace_services service
    JOIN service_categories category ON category.id = service.category_id
    LEFT JOIN service_questions question ON question.service_id = service.id
    WHERE service.active = TRUE
      AND category.active = TRUE
      AND category.code IN ('HOME', 'TECHNOLOGY', 'BUSINESS', 'EDUCATION')
      AND NOT EXISTS (
          SELECT 1
          FROM service_questions existing
          WHERE existing.service_id = service.id
            AND existing.question_type = 'LOCATION'
            AND existing.active = TRUE
      )
    GROUP BY service.id, category.code
)
INSERT INTO service_questions (
    service_id,
    question_key,
    prompt,
    helper_text,
    question_type,
    display_order,
    condition_question_key,
    condition_value
)
SELECT
    missing.service_id,
    'service_location',
    'Which municipality or city is the work in?',
    'Do not enter a street address. The exact address can be shared after hiring.',
    'LOCATION',
    missing.last_display_order + 2,
    CASE WHEN missing.category_code IN ('TECHNOLOGY', 'BUSINESS', 'EDUCATION')
        THEN 'delivery_mode' END,
    CASE WHEN missing.category_code IN ('TECHNOLOGY', 'BUSINESS', 'EDUCATION')
        THEN 'ON_SITE' END
FROM services_missing_location missing
ON CONFLICT (service_id, question_key) DO NOTHING;

INSERT INTO service_questions (
    service_id,
    question_key,
    prompt,
    helper_text,
    question_type,
    display_order,
    condition_question_key,
    condition_value
)
SELECT
    location.service_id,
    'service_postcode',
    'What is the postcode for this location?',
    'Enter five digits, for example 418 33. Do not enter a street address.',
    'POSTCODE',
    location.display_order + 1,
    location.condition_question_key,
    location.condition_value
FROM service_questions location
WHERE location.question_key = 'service_location'
  AND location.active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM service_questions postcode
      WHERE postcode.service_id = location.service_id
        AND postcode.question_key = 'service_postcode'
  )
ON CONFLICT (service_id, question_key) DO NOTHING;
