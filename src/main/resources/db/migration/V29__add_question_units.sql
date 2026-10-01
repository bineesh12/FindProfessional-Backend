ALTER TABLE service_questions
    ADD COLUMN unit VARCHAR(32);

UPDATE service_questions
SET unit = 'm²'
WHERE question_key IN ('project_size', 'property_size', 'floor_area', 'garden_size', 'area_size')
  AND question_type = 'NUMBER';
