-- Admin-controlled release of finalized assessment results and feedback.
ALTER TABLE assessments
    ADD COLUMN IF NOT EXISTS results_visible BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE assessments
    ADD COLUMN IF NOT EXISTS feedback_visible BOOLEAN NOT NULL DEFAULT FALSE;
