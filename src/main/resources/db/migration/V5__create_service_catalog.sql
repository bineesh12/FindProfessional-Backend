CREATE TABLE service_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    icon_key VARCHAR(64) NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE marketplace_services (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES service_categories(id),
    code VARCHAR(96) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    short_description VARCHAR(240) NOT NULL,
    icon_key VARCHAR(64) NOT NULL,
    image_url TEXT,
    search_keywords TEXT NOT NULL DEFAULT '',
    popular BOOLEAN NOT NULL DEFAULT FALSE,
    popularity_rank INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_categories_active_order
    ON service_categories(active, display_order);
CREATE INDEX idx_marketplace_services_category_active
    ON marketplace_services(category_id, active);
CREATE INDEX idx_marketplace_services_popular_rank
    ON marketplace_services(popular, popularity_rank)
    WHERE active = TRUE;

INSERT INTO service_categories (id, code, name, icon_key, display_order) VALUES
    ('10000000-0000-0000-0000-000000000001', 'HOME', 'Home', 'home', 1),
    ('10000000-0000-0000-0000-000000000002', 'VEHICLE', 'Vehicle', 'directions_car', 2),
    ('10000000-0000-0000-0000-000000000003', 'MOVING', 'Moving', 'local_shipping', 3),
    ('10000000-0000-0000-0000-000000000004', 'TECHNOLOGY', 'Technology', 'computer', 4),
    ('10000000-0000-0000-0000-000000000005', 'BUSINESS', 'Business', 'business_center', 5),
    ('10000000-0000-0000-0000-000000000006', 'EVENTS', 'Events', 'event', 6),
    ('10000000-0000-0000-0000-000000000007', 'EDUCATION', 'Education', 'school', 7),
    ('10000000-0000-0000-0000-000000000008', 'GARDEN', 'Garden', 'yard', 8),
    ('10000000-0000-0000-0000-000000000009', 'OTHER', 'Other', 'more_horiz', 9);

INSERT INTO marketplace_services (
    id, category_id, code, name, short_description, icon_key, image_url,
    search_keywords, popular, popularity_rank
) VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        'HOME_RENOVATION', 'Home renovation', 'Top rated professionals', 'construction',
        'https://lh3.googleusercontent.com/aida-public/AB6AXuAyeJv82870nKedAnJsPXsGC-pKZqeFAtn1v9HploBOtUQXehikNOKqc1wkgUCoMXbS0MbgOhlWIIAnriqu2y1XzG_YDX3Yd4SPHw9MwGt1n4CKuS7BVlKWFx4rT7qgZHKKtVoYhwKrspAWP2RcVKp-Pycf4IQ5bwAR_rV-SWIA-kbh8byh5duItH1wSdMvQk9QlnI1-2bJi6Hkuf9vOnWaKJNnENTKsWh43MKp24MQxKoJ2xyQsN4C',
        'renovate remodeling builder house room kitchen bathroom roof', TRUE, 1
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        'HOME_CLEANING', 'Cleaning', 'Spotless homes', 'cleaning_services',
        'https://lh3.googleusercontent.com/aida-public/AB6AXuBaxDUXaLKEwRQm9PgNJGOE3fDBgLiR9DMz9v_MHQOO66KtB25PTIKNB6LrbWscGsNyH4awY6dULm8RwCn78bf-cjhwAaa244E6NbA4ASA_eudHMPS8Em-ItCOTe0Hd-7DngX65waIYNyDa0mO5nYnRVzp2PBEew1OeC8iV7tNDI22THY4lArExrEA34Wbhz3BgRJ4JWwKiX0Xx816R7PtXsjXzSIToibU1w6VWdz2MZD5yclo5ql_v',
        'clean cleaner housekeeping apartment office move out', TRUE, 2
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000003',
        'MOVING_HELP', 'Moving', 'Local and long distance', 'local_shipping',
        'https://lh3.googleusercontent.com/aida-public/AB6AXuC8KZ88tpzFWAmphvonubndyXBA2dslP_AqVrE7plyE1l8VaoLcd4hjtpHo8Ipdd4HRphkXUXxEsY7Ob3_q-DTFBb87877Xb0htqlNOmYyXkc4IH9kAmxJSCYED8hQvxghewTh2YMW4nCV5FKXLG4uQL1Pcpq5PhL-zYBfSsSMKtH_qABMVY5bKO3OgTWXBEYLhYkYcXDC1bNp3odSx8CublsADKD_wOsHAVWr5NerG7CdTgQEruNQh',
        'move mover packing transport furniture relocation', TRUE, 3
    ),
    (
        '20000000-0000-0000-0000-000000000004',
        '10000000-0000-0000-0000-000000000002',
        'CAR_REPAIR', 'Car repair', 'Expert mechanics', 'car_repair',
        'https://lh3.googleusercontent.com/aida-public/AB6AXuCs1TRhWurAiQIjsnwJD7Zg12vNsBzcreTpzRvPxvk96ZJ-923MBvd1S7YAfYosmD15fUVnu3-n03EiGKatioIlGW6nOeEuzs8XO1dbShy7_ZSjN_TEqm8iJzVfPLR7pdwaC2HtxhugiN1hb6aWBf1-ze9yiAN8Gc4zOsOu5Ay9PHY9qMVE3cATwzMiIIT9XI2k6hJ-xhCj95KBl4SJvhML0G9Ho5lvo0Hb-H34ug7nvuzyYJ57R79R',
        'vehicle mechanic auto garage maintenance electric car', TRUE, 4
    ),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'PLUMBING', 'Plumbing', 'Repairs, installation and maintenance', 'plumbing', NULL, 'plumber pipe leak drain tap water', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'ELECTRICAL', 'Electrical work', 'Safe electrical installation and repair', 'electrical_services', NULL, 'electrician wiring lights power socket', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'PAINTING', 'Painting', 'Interior and exterior painting', 'format_paint', NULL, 'painter walls facade wallpaper', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000002', 'TIRE_SERVICE', 'Tire service', 'Seasonal changes and tire repair', 'tire_repair', NULL, 'car vehicle wheel tyre tire change', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000004', 'COMPUTER_SUPPORT', 'Computer support', 'Setup, troubleshooting and repair', 'computer', NULL, 'laptop pc wifi network software repair', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000005', 'ACCOUNTING', 'Accounting', 'Bookkeeping and business support', 'calculate', NULL, 'accountant tax bookkeeping payroll company', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000006', 'EVENT_PHOTOGRAPHY', 'Event photography', 'Professional coverage for your event', 'photo_camera', NULL, 'photographer wedding party conference photo', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000007', 'TUTORING', 'Tutoring', 'Personal learning support', 'school', NULL, 'teacher tutor lessons mathematics language', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000008', 'GARDEN_MAINTENANCE', 'Garden maintenance', 'Seasonal care and landscaping', 'yard', NULL, 'gardener lawn hedge landscaping tree', FALSE, NULL),
    ('20000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000009', 'GENERAL_HELP', 'General help', 'Tell us what you need done', 'handyman', NULL, 'other general handyman help task', FALSE, NULL);
