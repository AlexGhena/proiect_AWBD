-- Spring Security remember-me infrastructure table (PersistentTokenBasedRememberMeServices).
-- This is framework infrastructure, not a domain entity: it is deliberately not mapped as JPA
-- and does not change the seven-entity domain model.

CREATE TABLE persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL
);

-- Logout revokes every series for a user, so that lookup must not be a sequential scan.
CREATE INDEX idx_persistent_logins_username ON persistent_logins (username);
