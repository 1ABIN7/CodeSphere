-- The current Question entity is stored in `questions`. Early schema versions
-- referenced the retired `question_bank` table, which prevents newly-created
-- Question Bank entries from being added to an assessment.
DO $$
DECLARE
    dependency_fk RECORD;
BEGIN
    FOR dependency_fk IN
        SELECT c.conname, child.relname AS table_name
        FROM pg_constraint c
        JOIN pg_class child ON child.oid = c.conrelid
        WHERE c.contype = 'f'
          AND c.confrelid = 'question_bank'::regclass
          AND child.relname IN ('assessment_questions', 'session_answers')
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', dependency_fk.table_name, dependency_fk.conname);
    END LOOP;
END $$;

ALTER TABLE assessment_questions
    ADD CONSTRAINT assessment_questions_question_bank_id_fkey
    FOREIGN KEY (question_bank_id) REFERENCES questions(id) ON DELETE CASCADE;

ALTER TABLE session_answers
    ADD CONSTRAINT session_answers_question_id_fkey
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;
