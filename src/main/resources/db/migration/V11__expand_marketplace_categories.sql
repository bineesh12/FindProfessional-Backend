WITH service_seed(category_code, code, name, short_description, icon_key, search_keywords) AS (
    VALUES
        ('VEHICLE', 'CAR_DETAILING', 'Car detailing', 'Interior and exterior vehicle cleaning', 'car_repair', 'car auto vehicle detailing wash polish interior exterior clean'),
        ('VEHICLE', 'VEHICLE_DIAGNOSTICS', 'Vehicle diagnostics', 'Identify warning lights and vehicle faults', 'car_repair', 'car vehicle diagnostics warning light fault engine battery inspection'),
        ('VEHICLE', 'TOWING_ROADSIDE', 'Towing and roadside help', 'Help with breakdowns and vehicle transport', 'local_shipping', 'tow towing roadside breakdown jump start transport recovery'),
        ('MOVING', 'PACKING_HELP', 'Packing help', 'Packing and unpacking for a move', 'local_shipping', 'packing unpacking boxes move moving relocation'),
        ('MOVING', 'FURNITURE_ASSEMBLY', 'Furniture assembly', 'Assembly and disassembly of furniture', 'handyman', 'furniture assemble assembly disassemble wardrobe bed table cabinet'),
        ('MOVING', 'JUNK_REMOVAL', 'Junk removal', 'Remove unwanted furniture and household items', 'local_shipping', 'junk waste rubbish removal furniture disposal clear out haul'),
        ('TECHNOLOGY', 'PHONE_TABLET_REPAIR', 'Phone and tablet repair', 'Repair mobile devices and tablets', 'computer', 'phone mobile smartphone tablet screen battery charging repair'),
        ('TECHNOLOGY', 'NETWORK_SETUP', 'Network and Wi-Fi setup', 'Home and business network support', 'computer', 'wifi wi-fi internet router network setup connection coverage'),
        ('TECHNOLOGY', 'WEBSITE_DEVELOPMENT', 'Website development', 'Build or improve a website', 'computer', 'website web development ecommerce landing page wordpress frontend backend'),
        ('BUSINESS', 'DIGITAL_MARKETING', 'Digital marketing', 'Marketing campaigns and online growth', 'business_center', 'digital marketing advertising social media seo campaign content'),
        ('BUSINESS', 'BUSINESS_CONSULTING', 'Business consulting', 'Planning, operations and growth advice', 'business_center', 'business consultant strategy planning operations growth startup'),
        ('BUSINESS', 'ADMIN_SUPPORT', 'Administrative support', 'Flexible administrative and office assistance', 'business_center', 'administration admin assistant office data entry scheduling documents'),
        ('EVENTS', 'EVENT_PLANNING', 'Event planning', 'Plan and coordinate an event', 'event', 'event planner planning wedding party conference coordination venue'),
        ('EVENTS', 'EVENT_CATERING', 'Event catering', 'Food and service for events', 'event', 'catering caterer food buffet dinner wedding party event'),
        ('EVENTS', 'DJ_LIVE_MUSIC', 'DJ and live music', 'Music and entertainment for events', 'event', 'dj music band musician entertainment wedding party event'),
        ('EDUCATION', 'LANGUAGE_LESSONS', 'Language lessons', 'Private or group language learning', 'school', 'language teacher lessons english swedish spanish french tutoring'),
        ('EDUCATION', 'MUSIC_LESSONS', 'Music lessons', 'Learn an instrument or singing', 'school', 'music teacher lessons piano guitar singing violin instrument'),
        ('EDUCATION', 'CAREER_COACHING', 'Career coaching', 'Career planning, CV and interview support', 'school', 'career coach cv resume interview job application guidance'),
        ('GARDEN', 'LANDSCAPING', 'Landscaping', 'Design and improve outdoor spaces', 'yard', 'landscaping garden design patio paving plants outdoor'),
        ('GARDEN', 'TREE_CARE', 'Tree care', 'Tree pruning, assessment and removal', 'yard', 'tree arborist pruning trimming removal stump branches'),
        ('GARDEN', 'SNOW_REMOVAL', 'Snow removal', 'Clear snow and ice from outdoor areas', 'yard', 'snow ice removal clearing driveway path winter plough'),
        ('OTHER', 'HANDYMAN_SERVICE', 'Handyman', 'Help with small repairs and practical tasks', 'handyman', 'handyman repair mount install fix odd jobs household'),
        ('OTHER', 'PET_CARE', 'Pet care', 'Pet sitting, walking and practical pet help', 'handyman', 'pet dog cat sitting walking feeding care'),
        ('OTHER', 'PERSONAL_ASSISTANCE', 'Personal assistance', 'Practical help with everyday tasks', 'handyman', 'personal assistance errands shopping organizing practical help')
)
INSERT INTO marketplace_services (
    category_id, code, name, short_description, icon_key, search_keywords, popular
)
SELECT category.id, seed.code, seed.name, seed.short_description, seed.icon_key,
       seed.search_keywords, FALSE
FROM service_seed seed
JOIN service_categories category ON category.code = seed.category_code
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    icon_key = EXCLUDED.icon_key,
    search_keywords = EXCLUDED.search_keywords,
    active = TRUE;

WITH unconfigured_services AS (
    SELECT service.id, service.name, category.code AS category_code
    FROM marketplace_services service
    JOIN service_categories category ON category.id = service.category_id
    WHERE category.code <> 'HOME'
      AND NOT EXISTS (
          SELECT 1 FROM service_questions question WHERE question.service_id = service.id
      )
)
INSERT INTO service_questions (
    service_id, question_key, prompt, helper_text, question_type, display_order
)
SELECT id, 'task_details', 'What do you need help with?',
       'Include the result you want and any useful details.', 'TEXT', 1
FROM unconfigured_services
UNION ALL
SELECT id, 'preferred_date', 'When would you like the work to happen?',
       'An approximate date is enough.', 'DATE', 2
FROM unconfigured_services
UNION ALL
SELECT id, 'service_location', 'Which municipality or city is the work in?',
       'Do not enter a street address. The exact address can be shared after hiring.', 'LOCATION', 3
FROM unconfigured_services
WHERE category_code IN ('VEHICLE', 'MOVING', 'EVENTS', 'GARDEN', 'OTHER')
UNION ALL
SELECT id, 'delivery_mode', 'How should the service be delivered?',
       'Choose whether you prefer remote or on-site help.', 'SINGLE_CHOICE', 3
FROM unconfigured_services
WHERE category_code IN ('TECHNOLOGY', 'BUSINESS', 'EDUCATION');

INSERT INTO question_options (question_id, option_value, label, description, display_order)
SELECT question.id, option.option_value, option.label, option.description, option.display_order
FROM service_questions question
JOIN marketplace_services service ON service.id = question.service_id
JOIN service_categories category ON category.id = service.category_id
CROSS JOIN (
    VALUES
        ('REMOTE', 'Remote', 'The professional can provide the service online.', 1),
        ('ON_SITE', 'On site', 'The professional should come to the service location.', 2),
        ('FLEXIBLE', 'Flexible', 'Either remote or on-site delivery can work.', 3)
) AS option(option_value, label, description, display_order)
WHERE question.question_key = 'delivery_mode'
  AND category.code IN ('TECHNOLOGY', 'BUSINESS', 'EDUCATION')
ON CONFLICT (question_id, option_value) DO NOTHING;
