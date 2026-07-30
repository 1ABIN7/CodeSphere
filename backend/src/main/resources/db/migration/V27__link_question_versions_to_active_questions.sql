-- Question version history now tracks records in the active questions table.
ALTER TABLE question_versions DROP CONSTRAINT IF EXISTS question_versions_question_id_fkey;
ALTER TABLE question_versions
    ADD CONSTRAINT question_versions_question_id_fkey
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE NOT VALID;
