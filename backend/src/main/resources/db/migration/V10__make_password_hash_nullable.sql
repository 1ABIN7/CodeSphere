-- V10: Make legacy password_hash nullable
-- ============================================================================

ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
