-- H2 port of the transactionService schema, used only by the `test` profile.
-- Version-aligned with src/main/resources/db/migration (Postgres); differences are dialect only:
-- RANDOM_UUID() for gen_random_uuid(), REGEXP_LIKE() for the `~` operator, and
-- TIMESTAMP WITH TIME ZONE for TIMESTAMPTZ. Column comments are dropped.

CREATE TABLE transaction_categories (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scheduled_transactions (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    category_id UUID,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    next_execution_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scheduled_category
        FOREIGN KEY (category_id) REFERENCES transaction_categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_scheduled_amount CHECK (amount > 0),
    CONSTRAINT ck_scheduled_currency CHECK (REGEXP_LIKE(currency, '^[A-Z]{3}$')),
    CONSTRAINT ck_scheduled_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_scheduled_status CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    CONSTRAINT ck_scheduled_different_accounts CHECK (source_account_id <> destination_account_id)
);

CREATE TABLE bank_transactions (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    category_id UUID,
    scheduled_transaction_id UUID,
    source_account_id UUID,
    destination_account_id UUID,
    saga_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(255),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id) REFERENCES transaction_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_scheduled_transaction
        FOREIGN KEY (scheduled_transaction_id) REFERENCES scheduled_transactions (id) ON DELETE SET NULL,
    CONSTRAINT ck_transaction_amount CHECK (amount > 0),
    CONSTRAINT ck_transaction_currency CHECK (REGEXP_LIKE(currency, '^[A-Z]{3}$')),
    CONSTRAINT ck_transaction_type CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT ck_transaction_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'COMPENSATED')),
    CONSTRAINT ck_transaction_has_account CHECK (
        source_account_id IS NOT NULL OR destination_account_id IS NOT NULL
    ),
    CONSTRAINT ck_transaction_different_accounts CHECK (
        source_account_id IS NULL
        OR destination_account_id IS NULL
        OR source_account_id <> destination_account_id
    )
);

CREATE INDEX idx_transactions_category_id ON bank_transactions (category_id);
CREATE INDEX idx_transactions_scheduled_transaction_id ON bank_transactions (scheduled_transaction_id);
CREATE INDEX idx_transactions_source_account ON bank_transactions (source_account_id);
CREATE INDEX idx_transactions_destination_account ON bank_transactions (destination_account_id);
CREATE INDEX idx_transactions_created_at ON bank_transactions (created_at DESC);
CREATE INDEX idx_transactions_amount ON bank_transactions (amount);
CREATE INDEX idx_transactions_status ON bank_transactions (status);
CREATE INDEX idx_scheduled_category_id ON scheduled_transactions (category_id);
CREATE INDEX idx_scheduled_source_account ON scheduled_transactions (source_account_id);
CREATE INDEX idx_scheduled_next_execution_date ON scheduled_transactions (next_execution_date);
CREATE INDEX idx_scheduled_status ON scheduled_transactions (status);
