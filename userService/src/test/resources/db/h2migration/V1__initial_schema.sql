-- H2 port of the userService schema, used only by the `test` profile.
-- Kept version-aligned with src/main/resources/db/migration (Postgres); differences are
-- limited to dialect: RANDOM_UUID() for gen_random_uuid(), REGEXP_LIKE() for the `~` operator,
-- TIMESTAMP WITH TIME ZONE for TIMESTAMPTZ, and a generated marker column to emulate the
-- Postgres partial unique index "one default address per profile".

CREATE TABLE app_users (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_profiles (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE TABLE roles (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255),
    CONSTRAINT ck_role_name CHECK (REGEXP_LIKE(name, '^ROLE_[A-Z0-9_]+$'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE TABLE addresses (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    profile_id UUID NOT NULL,
    label VARCHAR(20) NOT NULL DEFAULT 'HOME',
    street VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    -- Emulates the Postgres partial unique index: non-default rows collapse to NULL (which unique
    -- indexes treat as distinct), so only one default per profile can exist.
    default_profile_marker UUID GENERATED ALWAYS AS (CASE WHEN is_default THEN profile_id END),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_address_profile
        FOREIGN KEY (profile_id) REFERENCES user_profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_address_label CHECK (label IN ('HOME', 'BILLING', 'WORK', 'OTHER')),
    CONSTRAINT ck_address_country CHECK (REGEXP_LIKE(country, '^[A-Z]{2}$'))
);

CREATE INDEX idx_app_users_created_at ON app_users (created_at DESC);
CREATE INDEX idx_app_users_enabled ON app_users (enabled);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_addresses_profile_id ON addresses (profile_id);
CREATE UNIQUE INDEX uq_addresses_default_per_profile ON addresses (default_profile_marker);
