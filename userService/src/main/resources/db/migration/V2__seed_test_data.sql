-- These AppUser ids match bankingService's bank_accounts.user_id seed data,
-- so the logical cross-service reference resolves to a real seeded user.
-- Seed passwords (bcrypt-hashed below): Elena=Passw0rd!23, Mihai=Passw0rd!24, Cristina=AdminP@ss25

INSERT INTO roles (name, description)
VALUES
    ('ROLE_USER', 'Regular banking user'),
    ('ROLE_ADMIN', 'Application administrator')
ON CONFLICT (name) DO NOTHING;

INSERT INTO app_users (id, username, email, password_hash, enabled)
VALUES
    ('12d9ff81-1d75-4c56-acdd-3597207412bc', 'elena.dumitrescu', 'elena.dumitrescu@example.com',
     '$2a$10$b318sJIHJccolIe2MP2bY.OW4t5Zya1DEaLYWTqE/NjMjMpUoFR4S', TRUE),
    ('af944ee3-769a-4c79-b946-235768c1b3bf', 'mihai.radulescu', 'mihai.radulescu@example.com',
     '$2a$10$xc8oPakZ3Xp2W.AlaW40cuDzDaRy5SwsX4ZCpYZf9ef3MfhajODX2', TRUE),
    ('ba3a83cd-a754-404b-a977-08f1c40dc4b6', 'cristina.ionescu', 'cristina.ionescu@example.com',
     '$2a$10$krsUjtRcWfwTcrrfm5wOGughITT3XI9yIIHVryJ79sI69IOue6WHC', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_profiles (id, user_id, first_name, last_name, phone)
VALUES
    ('954b306e-b7d0-42a0-973f-ce75c662307d', '12d9ff81-1d75-4c56-acdd-3597207412bc',
     'Elena', 'Dumitrescu', '+40712345678'),
    ('5b24d9e1-d6d2-490e-a762-2ea0c84c69d7', 'af944ee3-769a-4c79-b946-235768c1b3bf',
     'Mihai', 'Radulescu', '+40723456789'),
    ('24fd6df3-07bc-46a3-823a-1f02a4412732', 'ba3a83cd-a754-404b-a977-08f1c40dc4b6',
     'Cristina', 'Ionescu', '+40734567890')
ON CONFLICT (id) DO NOTHING;

INSERT INTO addresses (id, profile_id, label, street, city, postal_code, country, is_default)
VALUES
    ('89a9574c-619a-4cd0-b345-c5b50489fe66', '954b306e-b7d0-42a0-973f-ce75c662307d',
     'HOME', 'Strada Aviatorilor 23', 'Bucuresti', '011853', 'RO', TRUE),
    ('0e668777-831c-4c84-8aff-9becb0ef17b7', '954b306e-b7d0-42a0-973f-ce75c662307d',
     'BILLING', 'Bulevardul Unirii 45', 'Bucuresti', '030823', 'RO', FALSE),
    ('e1171214-082e-45f9-828d-48e3d7e7bf1b', '5b24d9e1-d6d2-490e-a762-2ea0c84c69d7',
     'HOME', 'Strada Memorandumului 12', 'Cluj-Napoca', '400114', 'RO', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '12d9ff81-1d75-4c56-acdd-3597207412bc', id FROM roles WHERE name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT 'af944ee3-769a-4c79-b946-235768c1b3bf', id FROM roles WHERE name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT 'ba3a83cd-a754-404b-a977-08f1c40dc4b6', id FROM roles WHERE name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
