-- Self-registration no longer activates an account outright: a new row starts PENDING and stays
-- disabled and roleless until an administrator approves or rejects it. Existing rows (seeded users,
-- anyone registered before this migration) are already active, so they backfill as APPROVED.
ALTER TABLE app_users ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE app_users ADD CONSTRAINT ck_app_users_approval_status
    CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

CREATE INDEX idx_app_users_approval_status ON app_users (approval_status) WHERE approval_status = 'PENDING';
