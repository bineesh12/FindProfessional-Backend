UPDATE service_questions question
SET question_key = 'moving_from',
    prompt = 'Which municipality or city is the vehicle currently in?',
    display_order = 80,
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'TOWING_ROADSIDE'
  AND question.question_key = 'service_location';

UPDATE service_questions question
SET question_key = 'pickup_postcode',
    prompt = 'What is the vehicle location postcode?',
    display_order = 81,
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'TOWING_ROADSIDE'
  AND question.question_key = 'service_postcode';

INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order,
    condition_question_key, condition_value
)
SELECT service.id, 'moving_to', 'Which municipality or city should the vehicle be taken to?',
       'Do not enter a street address.', 'LOCATION', 90,
       'roadside_assistance_type', 'TRANSPORT'
FROM marketplace_services service
WHERE service.code = 'TOWING_ROADSIDE'
ON CONFLICT (service_id, question_key) DO NOTHING;

INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order,
    condition_question_key, condition_value
)
SELECT service.id, 'destination_postcode', 'What is the destination postcode?',
       'Enter five digits, for example 418 33. Do not enter a street address.', 'POSTCODE', 91,
       'roadside_assistance_type', 'TRANSPORT'
FROM marketplace_services service
WHERE service.code = 'TOWING_ROADSIDE'
ON CONFLICT (service_id, question_key) DO NOTHING;

UPDATE service_questions question
SET condition_question_key = 'packing_scope',
    condition_value = 'UNPACKING,BOTH',
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'PACKING_HELP'
  AND question.question_key IN ('moving_to', 'destination_postcode');

UPDATE service_questions question
SET condition_question_key = 'help_delivery_mode',
    condition_value = 'PHYSICAL,FLEXIBLE',
    updated_at = NOW()
FROM marketplace_services service
WHERE question.service_id = service.id
  AND service.code = 'GENERAL_HELP'
  AND question.question_key IN ('service_location', 'service_postcode');
