ALTER TABLE customer_requests
    ADD COLUMN work_finished_at TIMESTAMPTZ,
    ADD COLUMN completed_at TIMESTAMPTZ;
