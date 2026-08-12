-- Technical ledger backing idempotent debit/credit/compensate calls from the transfer Saga.
-- Like user_roles in user_db, this is infrastructure plumbing, not a counted domain entity.
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    account_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    saga_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_idempotency_operation CHECK (operation IN ('DEBIT', 'CREDIT', 'COMPENSATE'))
);

CREATE INDEX idx_idempotency_saga_id ON idempotency_keys (saga_id);
CREATE INDEX idx_idempotency_account_id ON idempotency_keys (account_id);
