-- Question tags, versions, rubrics, and nested questions are owned by their
-- parent question. They must be cleaned up when an administrator deletes it.
DO $$
DECLARE
    dependency_fk RECORD;
BEGIN
    FOR dependency_fk IN
        SELECT c.conname, child.relname AS table_name
        FROM pg_constraint c
        JOIN pg_class child ON child.oid = c.conrelid
        WHERE c.contype = 'f'
          AND c.confrelid = 'questions'::regclass
          AND child.relname IN ('question_tags', 'question_versions', 'rubrics', 'questions')
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', dependency_fk.table_name, dependency_fk.conname);
    END LOOP;

    ALTER TABLE question_tags
        ADD CONSTRAINT fk_question_tags_question
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

    ALTER TABLE question_versions
        ADD CONSTRAINT fk_question_versions_question
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

    ALTER TABLE rubrics
        ADD CONSTRAINT fk_rubrics_question
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

    ALTER TABLE questions
        ADD CONSTRAINT fk_questions_parent_question
        FOREIGN KEY (parent_question_id) REFERENCES questions(id) ON DELETE CASCADE;
END $$;
