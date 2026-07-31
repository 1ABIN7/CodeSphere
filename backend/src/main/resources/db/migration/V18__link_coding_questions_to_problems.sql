-- Lets a reusable Question Bank entry point to the executable Problem Bank task.
ALTER TABLE questions ADD COLUMN IF NOT EXISTS coding_problem_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_questions_coding_problem'
    ) THEN
        ALTER TABLE questions
            ADD CONSTRAINT fk_questions_coding_problem
            FOREIGN KEY (coding_problem_id) REFERENCES problems(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_questions_coding_problem_id ON questions(coding_problem_id);
