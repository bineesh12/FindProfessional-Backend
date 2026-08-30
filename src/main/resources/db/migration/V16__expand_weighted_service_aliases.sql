WITH aliases(service_code, phrase, weight) AS (
    VALUES
        ('TOWING_ROADSIDE', 'car broke down', 100),
        ('TOWING_ROADSIDE', 'need towing', 95),
        ('PACKING_HELP', 'packing boxes', 100),
        ('NETWORK_SETUP', 'wifi router', 100),
        ('NETWORK_SETUP', 'router coverage', 95),
        ('DIGITAL_MARKETING', 'seo advertising', 100),
        ('DIGITAL_MARKETING', 'marketing campaign', 95),
        ('EVENT_CATERING', 'catering food', 100),
        ('LANGUAGE_LESSONS', 'spanish language', 100),
        ('LANGUAGE_LESSONS', 'swedish lessons', 100),
        ('TREE_CARE', 'prune branches', 100),
        ('TREE_CARE', 'large tree', 85),
        ('PET_CARE', 'walk dog', 100),
        ('PET_CARE', 'pet sitting', 100)
)
INSERT INTO service_aliases (service_id, phrase, weight)
SELECT service.id, alias.phrase, alias.weight
FROM aliases alias
JOIN marketplace_services service ON service.code = alias.service_code
ON CONFLICT (service_id, phrase) DO UPDATE SET
    weight = EXCLUDED.weight,
    active = TRUE;
