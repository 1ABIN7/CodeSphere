ALTER TABLE questions ADD COLUMN IF NOT EXISTS system_generated BOOLEAN NOT NULL DEFAULT FALSE;

-- Any Question Bank entry linked to a Problem Bank task was created by the
-- curated sync and is intentionally read-only while still assignable.
UPDATE questions SET system_generated = TRUE WHERE coding_problem_id IS NOT NULL;
