-- H2 port of the bankingService schema, used only by the `test` profile.
-- Version-aligned with src/main/resources/db/migration (Postgres); differences are dialect only:
-- RANDOM_UUID() for gen_random_uuid(), REGEXP_LIKE() for the `~` operator, and
-- TIMESTAMP WITH TIME ZONE for TIMESTAMPTZ. Column comments are dropped.

CREATE TABLE bank_accounts (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL,
    iban VARCHAR(34) NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_account_currency CHECK (REGEXP_LIKE(currency, '^[A-Z]{3}$')),
    CONSTRAINT ck_account_balance CHECK (balance >= 0),
    CONSTRAINT ck_account_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

CREATE TABLE bank_cards (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    account_id UUID NOT NULL,
    card_reference VARCHAR(64) NOT NULL UNIQUE,
    last_four VARCHAR(4) NOT NULL,
    cardholder_name VARCHAR(100) NOT NULL,
    expiry_month SMALLINT NOT NULL,
    expiry_year SMALLINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_account
        FOREIGN KEY (account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT ck_card_last_four CHECK (REGEXP_LIKE(last_four, '^[0-9]{4}$')),
    CONSTRAINT ck_card_expiry_month CHECK (expiry_month BETWEEN 1 AND 12),
    CONSTRAINT ck_card_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))
);

CREATE TABLE beneficiaries (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    owner_account_id UUID NOT NULL,
    beneficiary_name VARCHAR(100) NOT NULL,
    beneficiary_iban VARCHAR(34) NOT NULL,
    nickname VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_beneficiary_owner_account
        FOREIGN KEY (owner_account_id) REFERENCES bank_accounts (id) ON DELETE CASCADE,
    CONSTRAINT uq_beneficiary_per_owner UNIQUE (owner_account_id, beneficiary_iban)
);

CREATE INDEX idx_accounts_user_id ON bank_accounts (user_id);
CREATE INDEX idx_accounts_created_at ON bank_accounts (created_at DESC);
CREATE INDEX idx_accounts_balance ON bank_accounts (balance);
CREATE INDEX idx_cards_account_id ON bank_cards (account_id);
CREATE INDEX idx_beneficiaries_owner_account_id ON beneficiaries (owner_account_id);
