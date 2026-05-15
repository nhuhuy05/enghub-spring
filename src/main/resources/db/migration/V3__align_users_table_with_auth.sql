-- V3__align_users_table_with_auth.sql
-- Align users table with current authentication/user entities

-- 1) Add password column used by email/password authentication.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- 2) Ensure provider/provider_id pair is unique only when provider_id is present.
--    V1 already has a regular unique constraint, keep it as-is for compatibility.

-- 3) Helpful index for provider-based login lookup (OAuth/social login).
CREATE INDEX IF NOT EXISTS idx_users_provider_provider_id
    ON users (provider, provider_id);
