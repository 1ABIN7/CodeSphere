-- V7: Align Users Schema with JPA Entity
-- ============================================================================

-- 1. Add missing columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_password_token_expiry TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255);

-- 2. Migrate existing data (username from email if empty, password from password_hash)
UPDATE users SET username = split_part(email, '@', 1) WHERE username IS NULL;
UPDATE users SET password = password_hash WHERE password IS NULL;
UPDATE users SET role = 'ROLE_CANDIDATE' WHERE role IS NULL;

-- 3. Make username and password non-null
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN password SET NOT NULL;
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

-- 4. Add unique constraint to username
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);

-- Note: We are keeping the old roles and user_roles tables, as well as password_hash 
-- column for backward compatibility, but JPA will no longer use them.

-- 5. Seed new roles into the legacy roles table just in case they are referenced somewhere
INSERT INTO roles (name) VALUES ('ROLE_SUPER_ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_ORG_ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_EXAMINER') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_INSTRUCTOR') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_CANDIDATE') ON CONFLICT DO NOTHING;
