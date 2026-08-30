INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order
)
SELECT service.id,
       'task_details',
       'Describe what you need help with.',
       'Include the result you want and any useful details.',
       'TEXT',
       10
FROM marketplace_services service
WHERE service.active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM service_questions question
      WHERE question.service_id = service.id
        AND question.question_key = 'task_details'
  )
ON CONFLICT (service_id, question_key) DO NOTHING;
