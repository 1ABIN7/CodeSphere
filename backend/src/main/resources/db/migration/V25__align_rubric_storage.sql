-- Earlier schema versions stored a JSON criteria column in rubrics. The active
-- application stores each criterion in rubric_criteria, linked to rubrics.id.
ALTER TABLE rubrics ALTER COLUMN criteria DROP NOT NULL;

CREATE TABLE IF NOT EXISTS rubric_criteria (
    id BIGSERIAL PRIMARY KEY,
    criterion_name VARCHAR(255) NOT NULL,
    max_points INTEGER NOT NULL,
    rubric_id BIGINT NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rubrics_question_id ON rubrics(question_id);
