INSERT INTO marketplace_services (
    id, category_id, code, name, short_description, icon_key, search_keywords, popular
)
SELECT gen_random_uuid(), category.id, 'ROOFING', 'Roofing',
       'Roof repair, replacement and maintenance', 'roofing',
       'roof roofing replace repair leak storm tile metal inspection maintenance', FALSE
FROM service_categories category
WHERE category.code = 'HOME'
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    icon_key = EXCLUDED.icon_key,
    search_keywords = EXCLUDED.search_keywords,
    active = TRUE;

WITH definitions(question_key, prompt, question_type, display_order) AS (
    VALUES
        ('task_details','Describe what you need help with.','TEXT',10),
        ('roofing_work_type','What roofing work do you need?','SINGLE_CHOICE',20),
        ('property_type','What type of property is it?','SINGLE_CHOICE',30),
        ('active_damage','Is there an active leak or recent storm damage?','BOOLEAN',40),
        ('roof_material','What is the roof material, if known?','TEXT',50),
        ('preferred_date','When would you like the roofing work done?','DATE',70),
        ('service_location','Which municipality or city is the property in?','LOCATION',80),
        ('service_postcode','What is the postcode for this location?','POSTCODE',81)
)
INSERT INTO service_questions (service_id, question_key, prompt, helper_text, question_type, display_order)
SELECT service.id, definition.question_key, definition.prompt,
       CASE WHEN definition.question_key = 'service_postcode'
           THEN 'Enter five digits, for example 418 33. Do not enter a street address.' END,
       definition.question_type, definition.display_order
FROM definitions definition
CROSS JOIN marketplace_services service
WHERE service.code = 'ROOFING'
ON CONFLICT (service_id, question_key) DO UPDATE SET
    prompt = EXCLUDED.prompt,
    helper_text = EXCLUDED.helper_text,
    question_type = EXCLUDED.question_type,
    display_order = EXCLUDED.display_order,
    active = TRUE,
    updated_at = NOW();

WITH options(question_key, option_value, label, display_order) AS (
    VALUES
        ('roofing_work_type','REPAIR','Repair',1),('roofing_work_type','REPLACE','Replace',2),
        ('roofing_work_type','MAINTENANCE','Maintenance',3),('roofing_work_type','INSPECTION_UNSURE','Inspection or not sure',4),
        ('property_type','HOUSE','House',1),('property_type','APARTMENT_BUILDING','Apartment building',2),
        ('property_type','COMMERCIAL','Commercial property',3),('property_type','OTHER','Other',4)
)
INSERT INTO question_options (question_id, option_value, label, display_order)
SELECT question.id, option.option_value, option.label, option.display_order
FROM options option
JOIN service_questions question ON question.question_key = option.question_key
JOIN marketplace_services service ON service.id = question.service_id AND service.code = 'ROOFING'
ON CONFLICT (question_id, option_value) DO UPDATE SET
    label = EXCLUDED.label,
    display_order = EXCLUDED.display_order;

INSERT INTO service_aliases (service_id, phrase, weight)
SELECT service.id, alias.phrase, alias.weight
FROM marketplace_services service
CROSS JOIN (VALUES
    ('change my roof',100),('replace roof',100),('roof leak',100),('repair roof',95),('roofing',90)
) AS alias(phrase, weight)
WHERE service.code = 'ROOFING'
ON CONFLICT (service_id, phrase) DO UPDATE SET weight = EXCLUDED.weight, active = TRUE;
