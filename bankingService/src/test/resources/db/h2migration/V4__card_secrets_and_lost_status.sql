-- Encrypted card secrets (AES-256-GCM ciphertext, base64) and the LOST_STOLEN status.
-- H2 needs one ALTER per column rather than Postgres' comma-separated ADD COLUMN list.
ALTER TABLE bank_cards ADD COLUMN card_number_encrypted VARCHAR(4096);
ALTER TABLE bank_cards ADD COLUMN cvv_encrypted VARCHAR(4096);
ALTER TABLE bank_cards ADD COLUMN pin_encrypted VARCHAR(4096);

ALTER TABLE bank_cards DROP CONSTRAINT ck_card_status;

ALTER TABLE bank_cards
    ADD CONSTRAINT ck_card_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED', 'LOST_STOLEN'));
