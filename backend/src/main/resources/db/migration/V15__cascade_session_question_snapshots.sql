-- Session snapshots are owned by an assessment session. Without a cascading
-- foreign key, deleting an assessment can fail after the database tries to
-- remove its sessions.
DO $$
DECLARE
    snapshot_fk TEXT;
BEGIN
    SELECT conname INTO snapshot_fk
    FROM pg_constraint
    WHERE conrelid = 'session_question_snapshots'::regclass
      AND contype = 'f'
      AND confrelid = 'assessment_sessions'::regclass
    LIMIT 1;

    IF snapshot_fk IS NOT NULL THEN
        EXECUTE format('ALTER TABLE session_question_snapshots DROP CONSTRAINT %I', snapshot_fk);
    END IF;

    ALTER TABLE session_question_snapshots
        ADD CONSTRAINT fk_session_question_snapshots_session
        FOREIGN KEY (session_id) REFERENCES assessment_sessions(id) ON DELETE CASCADE;
END $$;
