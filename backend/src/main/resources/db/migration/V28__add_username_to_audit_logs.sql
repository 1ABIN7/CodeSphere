-- Keep an account label alongside audit events so administrators can identify
-- the actor without exposing email addresses in the security activity feed.
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS username VARCHAR(255);
