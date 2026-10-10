ALTER TABLE postcode_coordinates
    ADD COLUMN municipality VARCHAR(160);

CREATE INDEX idx_postcode_coordinates_municipality_postcode
    ON postcode_coordinates(LOWER(municipality), postal_code);
