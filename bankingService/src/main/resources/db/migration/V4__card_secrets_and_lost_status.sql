ALTER TABLE bank_cards
    ADD COLUMN card_number_encrypted TEXT,
    ADD COLUMN cvv_encrypted TEXT,
    ADD COLUMN pin_encrypted TEXT;

COMMENT ON COLUMN bank_cards.card_number_encrypted IS
    'AES-256-GCM ciphertext of the full PAN, base64-encoded with a random IV prefix. NULL for cards issued before this column existed - backfilled lazily on first reveal.';
COMMENT ON COLUMN bank_cards.pin_encrypted IS
    'AES-256-GCM ciphertext of the 4-digit PIN, base64-encoded with a random IV prefix.';

ALTER TABLE bank_cards
    DROP CONSTRAINT ck_card_status;

ALTER TABLE bank_cards
    ADD CONSTRAINT ck_card_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED', 'LOST_STOLEN'));
