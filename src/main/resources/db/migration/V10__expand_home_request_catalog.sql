ALTER TABLE request_sessions
    ADD COLUMN draft_title VARCHAR(180),
    ADD COLUMN draft_description TEXT;

UPDATE marketplace_services
SET search_keywords = 'clean cleaner housekeeping apartment house office move out deep regular window tidy maid'
WHERE code = 'HOME_CLEANING';

UPDATE marketplace_services
SET search_keywords = 'plumber plumbing pipe leak leaking drain drainage blocked clog tap toilet sink water sewage'
WHERE code = 'PLUMBING';

UPDATE marketplace_services
SET search_keywords = 'electric electrician electrical wiring light lighting power socket outlet fuse breaker installation'
WHERE code = 'ELECTRICAL';

INSERT INTO marketplace_services (
    category_id, code, name, short_description, icon_key, search_keywords, popular
)
SELECT id, 'FLOORING', 'Flooring', 'Floor installation and repair', 'layers',
       'floor flooring floorboard hardwood wood laminate tile vinyl repair install replace', FALSE
FROM service_categories WHERE code = 'HOME'
ON CONFLICT (code) DO UPDATE SET
    search_keywords = EXCLUDED.search_keywords,
    short_description = EXCLUDED.short_description;

INSERT INTO marketplace_services (
    category_id, code, name, short_description, icon_key, search_keywords, popular
)
SELECT id, 'APPLIANCE_REPAIR', 'Appliance repair', 'Repair household appliances', 'home_repair_service',
       'appliance repair washing machine washer dishwasher refrigerator fridge freezer oven dryer broken leaking', FALSE
FROM service_categories WHERE code = 'HOME'
ON CONFLICT (code) DO UPDATE SET
    search_keywords = EXCLUDED.search_keywords,
    short_description = EXCLUDED.short_description;

WITH definitions(service_code, question_key, prompt, helper_text, question_type, display_order) AS (
    VALUES
        ('HOME_CLEANING', 'cleaning_type', 'What kind of cleaning do you need?', 'Choose the closest match.', 'SINGLE_CHOICE', 1),
        ('HOME_CLEANING', 'property_type', 'What type of property should be cleaned?', NULL, 'SINGLE_CHOICE', 2),
        ('HOME_CLEANING', 'property_size', 'Approximately how large is the property?', 'Enter the area in square metres.', 'NUMBER', 3),
        ('HOME_CLEANING', 'preferred_date', 'When would you like the cleaning?', 'An approximate date is enough.', 'DATE', 4),
        ('PLUMBING', 'issue_type', 'What plumbing problem do you need help with?', 'Choose the closest match.', 'SINGLE_CHOICE', 1),
        ('PLUMBING', 'affected_area', 'Where is the problem?', NULL, 'SINGLE_CHOICE', 2),
        ('PLUMBING', 'urgent', 'Does this need urgent attention?', 'Choose yes for active leaks, flooding, or loss of water.', 'BOOLEAN', 3),
        ('PLUMBING', 'service_location', 'Where is the property?', 'Enter the city or municipality.', 'LOCATION', 4),
        ('ELECTRICAL', 'issue_type', 'What electrical work do you need?', 'Choose the closest match.', 'SINGLE_CHOICE', 1),
        ('ELECTRICAL', 'property_type', 'What type of property is it?', NULL, 'SINGLE_CHOICE', 2),
        ('ELECTRICAL', 'urgent', 'Does this need urgent attention?', 'Choose yes if there are sparks, burning smells, or a complete power loss.', 'BOOLEAN', 3),
        ('ELECTRICAL', 'service_location', 'Where is the property?', 'Enter the city or municipality.', 'LOCATION', 4),
        ('FLOORING', 'work_type', 'What should be done with the floor?', NULL, 'SINGLE_CHOICE', 1),
        ('FLOORING', 'floor_material', 'Which flooring material is involved?', 'Choose unsure if you need professional advice.', 'SINGLE_CHOICE', 2),
        ('FLOORING', 'floor_area', 'Approximately how large is the floor area?', 'Enter the area in square metres.', 'NUMBER', 3),
        ('FLOORING', 'service_location', 'Where is the property?', 'Enter the city or municipality.', 'LOCATION', 4),
        ('APPLIANCE_REPAIR', 'appliance_type', 'Which appliance needs repair?', NULL, 'SINGLE_CHOICE', 1),
        ('APPLIANCE_REPAIR', 'brand_model', 'What is the brand and model?', 'Enter what you can find, or write unknown.', 'TEXT', 2),
        ('APPLIANCE_REPAIR', 'problem_description', 'What is happening with the appliance?', 'Include any error code, unusual noise, or leak.', 'TEXT', 3),
        ('APPLIANCE_REPAIR', 'urgent', 'Do you need an urgent visit?', NULL, 'BOOLEAN', 4),
        ('APPLIANCE_REPAIR', 'service_location', 'Where is the appliance?', 'Enter the city or municipality.', 'LOCATION', 5)
)
INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order
)
SELECT service.id, definition.question_key, definition.prompt, definition.helper_text,
       definition.question_type, definition.display_order
FROM definitions definition
JOIN marketplace_services service ON service.code = definition.service_code
ON CONFLICT (service_id, question_key) DO UPDATE SET
    prompt = EXCLUDED.prompt,
    helper_text = EXCLUDED.helper_text,
    question_type = EXCLUDED.question_type,
    display_order = EXCLUDED.display_order,
    active = TRUE;

WITH options(service_code, question_key, option_value, label, description, display_order) AS (
    VALUES
        ('HOME_CLEANING', 'cleaning_type', 'REGULAR', 'Regular cleaning', 'Recurring or routine home cleaning.', 1),
        ('HOME_CLEANING', 'cleaning_type', 'DEEP', 'Deep cleaning', 'A thorough one-time cleaning.', 2),
        ('HOME_CLEANING', 'cleaning_type', 'MOVE_OUT', 'Move-out cleaning', 'Cleaning before handing over a property.', 3),
        ('HOME_CLEANING', 'cleaning_type', 'WINDOW', 'Window cleaning', 'Interior or exterior window cleaning.', 4),
        ('HOME_CLEANING', 'property_type', 'APARTMENT', 'Apartment', NULL, 1),
        ('HOME_CLEANING', 'property_type', 'HOUSE', 'House', NULL, 2),
        ('HOME_CLEANING', 'property_type', 'OFFICE', 'Office', NULL, 3),
        ('HOME_CLEANING', 'property_type', 'OTHER', 'Other', NULL, 4),
        ('PLUMBING', 'issue_type', 'BLOCKED_DRAIN', 'Blocked drain', 'A sink, toilet, shower, or main drain is blocked.', 1),
        ('PLUMBING', 'issue_type', 'LEAK', 'Leak', 'Water is leaking from a pipe, fixture, or appliance connection.', 2),
        ('PLUMBING', 'issue_type', 'INSTALLATION', 'Installation', 'Install or replace a fixture or pipe.', 3),
        ('PLUMBING', 'issue_type', 'NO_WATER', 'No water or pressure', 'No water supply or unusually low pressure.', 4),
        ('PLUMBING', 'issue_type', 'OTHER', 'Other plumbing issue', NULL, 5),
        ('PLUMBING', 'affected_area', 'KITCHEN', 'Kitchen', NULL, 1),
        ('PLUMBING', 'affected_area', 'BATHROOM', 'Bathroom', NULL, 2),
        ('PLUMBING', 'affected_area', 'TOILET', 'Toilet', NULL, 3),
        ('PLUMBING', 'affected_area', 'BASEMENT', 'Basement or utility room', NULL, 4),
        ('PLUMBING', 'affected_area', 'OTHER', 'Other', NULL, 5),
        ('ELECTRICAL', 'issue_type', 'NO_POWER', 'No power', 'Complete or partial loss of power.', 1),
        ('ELECTRICAL', 'issue_type', 'OUTLET', 'Socket or outlet', 'Repair or install a socket.', 2),
        ('ELECTRICAL', 'issue_type', 'LIGHTING', 'Lighting', 'Repair or install lights and switches.', 3),
        ('ELECTRICAL', 'issue_type', 'INSTALLATION', 'New installation', 'New wiring, fuse box, charger, or equipment.', 4),
        ('ELECTRICAL', 'issue_type', 'OTHER', 'Other electrical work', NULL, 5),
        ('ELECTRICAL', 'property_type', 'APARTMENT', 'Apartment', NULL, 1),
        ('ELECTRICAL', 'property_type', 'HOUSE', 'House', NULL, 2),
        ('ELECTRICAL', 'property_type', 'COMMERCIAL', 'Commercial property', NULL, 3),
        ('ELECTRICAL', 'property_type', 'OTHER', 'Other', NULL, 4),
        ('FLOORING', 'work_type', 'REPAIR', 'Repair existing floor', NULL, 1),
        ('FLOORING', 'work_type', 'INSTALL', 'Install a new floor', NULL, 2),
        ('FLOORING', 'work_type', 'REMOVE_REPLACE', 'Remove and replace', NULL, 3),
        ('FLOORING', 'work_type', 'UNSURE', 'I am not sure', NULL, 4),
        ('FLOORING', 'floor_material', 'WOOD', 'Wood or parquet', NULL, 1),
        ('FLOORING', 'floor_material', 'LAMINATE', 'Laminate', NULL, 2),
        ('FLOORING', 'floor_material', 'TILE', 'Tile', NULL, 3),
        ('FLOORING', 'floor_material', 'VINYL', 'Vinyl', NULL, 4),
        ('FLOORING', 'floor_material', 'OTHER', 'Other or unsure', NULL, 5),
        ('APPLIANCE_REPAIR', 'appliance_type', 'WASHING_MACHINE', 'Washing machine', NULL, 1),
        ('APPLIANCE_REPAIR', 'appliance_type', 'DISHWASHER', 'Dishwasher', NULL, 2),
        ('APPLIANCE_REPAIR', 'appliance_type', 'REFRIGERATOR', 'Refrigerator or freezer', NULL, 3),
        ('APPLIANCE_REPAIR', 'appliance_type', 'OVEN', 'Oven or cooker', NULL, 4),
        ('APPLIANCE_REPAIR', 'appliance_type', 'DRYER', 'Tumble dryer', NULL, 5),
        ('APPLIANCE_REPAIR', 'appliance_type', 'OTHER', 'Other appliance', NULL, 6)
)
INSERT INTO question_options (question_id, option_value, label, description, display_order)
SELECT question.id, option.option_value, option.label, option.description, option.display_order
FROM options option
JOIN marketplace_services service ON service.code = option.service_code
JOIN service_questions question
  ON question.service_id = service.id AND question.question_key = option.question_key
ON CONFLICT (question_id, option_value) DO UPDATE SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    display_order = EXCLUDED.display_order;
