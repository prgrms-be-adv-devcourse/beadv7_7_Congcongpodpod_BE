ALTER TABLE payment_log
    DROP COLUMN raw_payload,
    ADD COLUMN payment_method TEXT,
    ADD COLUMN masked_card_num TEXT,
    ADD COLUMN card_company TEXT,
    ADD COLUMN failed_code VARCHAR(100),
    ADD COLUMN failed_message VARCHAR(500);