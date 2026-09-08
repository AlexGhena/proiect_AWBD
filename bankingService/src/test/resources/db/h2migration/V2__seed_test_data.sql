-- Seed data for the H2 test schema. Ids match the Postgres seed and the userService seed so the
-- logical cross-service references line up. ON CONFLICT is dropped: the test database is always fresh.
--   12d9ff81-1d75-4c56-acdd-3597207412bc = Elena Dumitrescu
--   af944ee3-769a-4c79-b946-235768c1b3bf = Mihai Radulescu

INSERT INTO bank_accounts (id, user_id, iban, currency, balance, status, version)
VALUES
    ('3255df08-8624-499c-8ab1-12f721653327', '12d9ff81-1d75-4c56-acdd-3597207412bc',
     'RO12BTRL09301202T0123456', 'RON', 4250.75, 'ACTIVE', 0),
    ('a0f3e83a-9bc2-4271-b67b-7c92580f1790', 'af944ee3-769a-4c79-b946-235768c1b3bf',
     'RO34INGB00000123456789ON', 'EUR', 1580.20, 'ACTIVE', 0),
    ('da1c92f8-1b1f-43df-b412-34bfb495a411', '12d9ff81-1d75-4c56-acdd-3597207412bc',
     'RO56BRDE450SV12345678901', 'RON', 0.00, 'BLOCKED', 0);

INSERT INTO bank_cards (id, account_id, card_reference, last_four, cardholder_name, expiry_month, expiry_year, status)
VALUES
    ('8d7d638c-3fa0-42b3-b1da-8302a4ea7eb3', '3255df08-8624-499c-8ab1-12f721653327',
     'TOK-4F2A9C1E7B3D', '7421', 'Elena Dumitrescu', 9, 2028, 'ACTIVE'),
    ('39110eab-5373-4602-94d9-ca85e075aecd', 'a0f3e83a-9bc2-4271-b67b-7c92580f1790',
     'TOK-9B31D8E4A210', '3390', 'Mihai Radulescu', 2, 2027, 'ACTIVE');

INSERT INTO beneficiaries (id, owner_account_id, beneficiary_name, beneficiary_iban, nickname)
VALUES
    ('3c1a6493-dfcc-4800-898e-deb7008a1133', '3255df08-8624-499c-8ab1-12f721653327',
     'Andrei Popescu', 'RO49RZBR0000060007654321', 'Landlord'),
    ('02f9b404-8fb8-41ab-ba2b-56060a728da2', '3255df08-8624-499c-8ab1-12f721653327',
     'Electrica Furnizare SA', 'RO88CECEB0000123456789RO', 'Electricity Bill');
