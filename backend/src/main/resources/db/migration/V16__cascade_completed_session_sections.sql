-- Completed-section markers are owned by an assessment session and must not
-- prevent an assessment from being removed.
DO $$
DECLARE
    completed_section_fk TEXT;
BEGIN
    SELECT conname INTO completed_section_fk
    FROM pg_constraint
    WHERE conrelid = 'session_completed_sections'::regclass
      AND contype = 'f'
      AND confrelid = 'assessment_sessions'::regclass
    LIMIT 1;

    IF completed_section_fk IS NOT NULL THEN
        EXECUTE format('ALTER TABLE session_completed_sections DROP CONSTRAINT %I', completed_section_fk);
    END IF;

    ALTER TABLE session_completed_sections
        ADD CONSTRAINT fk_session_completed_sections_session
        FOREIGN KEY (session_id) REFERENCES assessment_sessions(id) ON DELETE CASCADE;
END $$;
