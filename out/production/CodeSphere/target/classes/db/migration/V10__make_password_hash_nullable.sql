-- V10: Make legacy password_hash column nullable
-- ============================================================================

-- The V1 schema created password_hash as NOT NULL.
-- V7 added a new password column (NOT NULL) and we updated our JPA entity to use it.
-- We must make password_hash nullable so that JPA inserts do not fail.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
