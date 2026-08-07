-- Users are never hard-deleted: accounts, cards and transactions in the other services keep
-- referencing this row, so removing it would either orphan financial records or require a
-- cross-service cascade. Deletion instead stamps deleted_at and disables login.
ALTER TABLE app_users ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_app_users_deleted_at ON app_users (deleted_at) WHERE deleted_at IS NOT NULL;
