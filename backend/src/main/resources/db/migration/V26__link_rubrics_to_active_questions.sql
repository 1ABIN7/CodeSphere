-- Rubrics now belong to the active questions table, not the retired
-- question_bank table retained by the original schema.
ALTER TABLE rubrics DROP CONSTRAINT IF EXISTS rubrics_question_id_fkey;
ALTER TABLE rubrics
    ADD CONSTRAINT rubrics_question_id_fkey
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE NOT VALID;
