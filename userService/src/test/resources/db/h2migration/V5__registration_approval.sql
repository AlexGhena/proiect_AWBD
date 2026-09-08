-- Self-registered accounts start PENDING and stay disabled until an administrator decides.
-- Existing rows backfill as APPROVED.
ALTER TABLE app_users ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE app_users ADD CONSTRAINT ck_app_users_approval_status
    CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

CREATE INDEX idx_app_users_approval_status ON app_users (approval_status);
