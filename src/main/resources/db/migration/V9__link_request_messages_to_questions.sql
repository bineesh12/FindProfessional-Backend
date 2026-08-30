ALTER TABLE request_messages
    ADD COLUMN question_key VARCHAR(96);

CREATE UNIQUE INDEX uq_request_message_question_sender
    ON request_messages(session_id, question_key, sender)
    WHERE question_key IS NOT NULL;
