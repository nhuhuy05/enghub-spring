-- V5__create_invalidated_tokens.sql
-- Token blacklist storage for logout/revocation.

CREATE TABLE invalidated_tokens (
    jti VARCHAR(100) PRIMARY KEY,
    expiry_time TIMESTAMP NOT NULL,
    invalidated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invalidated_tokens_expiry_time ON invalidated_tokens(expiry_time);

