-- Users are soft-deleted: deletion stamps deleted_at instead of removing the row.
ALTER TABLE app_users ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_app_users_deleted_at ON app_users (deleted_at);
