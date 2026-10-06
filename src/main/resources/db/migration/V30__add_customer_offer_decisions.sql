ALTER TABLE professional_offers DROP CONSTRAINT professional_offers_status_valid;
ALTER TABLE professional_offers
    ADD CONSTRAINT professional_offers_status_valid
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'DECLINED', 'WITHDRAWN'));
