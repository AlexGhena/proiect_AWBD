CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    iban VARCHAR(34) NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_account_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_account_balance CHECK (balance >= 0),
    CONSTRAINT ck_account_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

COMMENT ON COLUMN bank_accounts.user_id IS
    'Logical AppUser reference owned and validated through userService; no cross-database foreign key.';

CREATE TABLE bank_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    card_reference VARCHAR(64) NOT NULL UNIQUE,
    last_four VARCHAR(4) NOT NULL,
    cardholder_name VARCHAR(100) NOT NULL,
    expiry_month SMALLINT NOT NULL,
    expiry_year SMALLINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_account
        FOREIGN KEY (account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT ck_card_last_four CHECK (last_four ~ '^[0-9]{4}$'),
    CONSTRAINT ck_card_expiry_month CHECK (expiry_month BETWEEN 1 AND 12),
    CONSTRAINT ck_card_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))
);

CREATE TABLE beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_account_id UUID NOT NULL,
    beneficiary_name VARCHAR(100) NOT NULL,
    beneficiary_iban VARCHAR(34) NOT NULL,
    nickname VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_beneficiary_owner_account
        FOREIGN KEY (owner_account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT uq_beneficiary_per_owner UNIQUE (owner_account_id, beneficiary_iban)
);

CREATE INDEX idx_accounts_user_id ON bank_accounts (user_id);
CREATE INDEX idx_accounts_created_at ON bank_accounts (created_at DESC);
CREATE INDEX idx_accounts_balance ON bank_accounts (balance);
CREATE INDEX idx_cards_account_id ON bank_cards (account_id);
CREATE INDEX idx_beneficiaries_owner_account_id ON beneficiaries (owner_account_id);
