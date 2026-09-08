-- Spring Security remember-me infrastructure table (PersistentTokenBasedRememberMeServices).
-- JdbcTokenRepositoryImpl issues unqualified SQL, so this must live in the connection's default
-- schema (PUBLIC for the H2 test profile).

CREATE TABLE persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL
);

CREATE INDEX idx_persistent_logins_username ON persistent_logins (username);
