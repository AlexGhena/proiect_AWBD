-- Seed data for the H2 test schema. Account ids match bankingService's seed so the logical
-- cross-service references line up. ON CONFLICT is dropped: the test database is always fresh.
--   3255df08-8624-499c-8ab1-12f721653327 = Elena Dumitrescu, RO12BTRL09301202T0123456 (RON, ACTIVE)
--   a0f3e83a-9bc2-4271-b67b-7c92580f1790 = Mihai Radulescu, RO34INGB00000123456789ON (EUR, ACTIVE)
--   da1c92f8-1b1f-43df-b412-34bfb495a411 = Elena Dumitrescu, RO56BRDE450SV12345678901 (RON, BLOCKED)

INSERT INTO transaction_categories (name, description)
VALUES
    ('TRANSFER', 'Transfer between bank accounts'),
    ('SALARY', 'Salary payment'),
    ('UTILITIES', 'Utility bill payment'),
    ('RENT', 'Recurring rent or shared housing expense'),
    ('GROCERIES', 'Everyday grocery and supermarket spending');

INSERT INTO scheduled_transactions
    (id, category_id, source_account_id, destination_account_id, amount, currency, frequency, next_execution_date, status, description)
SELECT
    '9ff480f0-c79b-44de-8928-476858d2a66a',
    (SELECT id FROM transaction_categories WHERE name = 'RENT'),
    '3255df08-8624-499c-8ab1-12f721653327',
    'a0f3e83a-9bc2-4271-b67b-7c92580f1790',
    400.00, 'RON', 'MONTHLY', DATE '2026-09-01', 'ACTIVE',
    'Monthly shared apartment expenses transfer'
WHERE NOT EXISTS (
    SELECT 1 FROM scheduled_transactions WHERE id = '9ff480f0-c79b-44de-8928-476858d2a66a'
);

INSERT INTO bank_transactions
    (id, category_id, scheduled_transaction_id, source_account_id, destination_account_id, saga_id,
     amount, currency, type, status, description, failure_reason)
SELECT
    '778eebf6-ca92-4682-ae6e-7c4c743d4171',
    (SELECT id FROM transaction_categories WHERE name = 'RENT'),
    '9ff480f0-c79b-44de-8928-476858d2a66a',
    '3255df08-8624-499c-8ab1-12f721653327',
    'a0f3e83a-9bc2-4271-b67b-7c92580f1790',
    '972844b8-c76b-4e0a-b25a-218c11408c6a',
    400.00, 'RON', 'TRANSFER', 'COMPLETED',
    'Monthly shared apartment expenses transfer - August 2026', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM bank_transactions WHERE id = '778eebf6-ca92-4682-ae6e-7c4c743d4171'
);

INSERT INTO bank_transactions
    (id, category_id, scheduled_transaction_id, source_account_id, destination_account_id, saga_id,
     amount, currency, type, status, description, failure_reason)
SELECT
    '6a6e40a6-8bba-4953-89c8-6dcf2a5756d3',
    (SELECT id FROM transaction_categories WHERE name = 'SALARY'),
    NULL,
    NULL,
    '3255df08-8624-499c-8ab1-12f721653327',
    '7cf6b867-19af-4bd0-bef6-49916ba9ad83',
    5200.00, 'RON', 'DEPOSIT', 'COMPLETED',
    'Salary payment - August 2026', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM bank_transactions WHERE id = '6a6e40a6-8bba-4953-89c8-6dcf2a5756d3'
);

INSERT INTO bank_transactions
    (id, category_id, scheduled_transaction_id, source_account_id, destination_account_id, saga_id,
     amount, currency, type, status, description, failure_reason)
SELECT
    '193fafc7-9fce-48de-9ee3-05f3725d6f5a',
    NULL,
    NULL,
    'a0f3e83a-9bc2-4271-b67b-7c92580f1790',
    NULL,
    '9ed32baf-701d-4fbe-b8bc-01b08bb53a57',
    89.99, 'EUR', 'WITHDRAWAL', 'COMPLETED',
    'ATM withdrawal - Henri Coanda Airport, Bucharest', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM bank_transactions WHERE id = '193fafc7-9fce-48de-9ee3-05f3725d6f5a'
);

INSERT INTO bank_transactions
    (id, category_id, scheduled_transaction_id, source_account_id, destination_account_id, saga_id,
     amount, currency, type, status, description, failure_reason)
SELECT
    'ab935cd9-f8fe-4175-b460-7aa0bac96c86',
    (SELECT id FROM transaction_categories WHERE name = 'TRANSFER'),
    NULL,
    '3255df08-8624-499c-8ab1-12f721653327',
    'da1c92f8-1b1f-43df-b412-34bfb495a411',
    '33e5cc5b-d76f-44ff-b3ea-4b728c5640cf',
    500.00, 'RON', 'TRANSFER', 'FAILED',
    'Attempted transfer to savings account', 'Destination account is BLOCKED'
WHERE NOT EXISTS (
    SELECT 1 FROM bank_transactions WHERE id = 'ab935cd9-f8fe-4175-b460-7aa0bac96c86'
);
