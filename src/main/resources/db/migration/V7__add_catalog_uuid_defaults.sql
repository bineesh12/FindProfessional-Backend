ALTER TABLE service_categories
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE marketplace_services
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE service_availability_areas
    ALTER COLUMN id SET DEFAULT gen_random_uuid();
