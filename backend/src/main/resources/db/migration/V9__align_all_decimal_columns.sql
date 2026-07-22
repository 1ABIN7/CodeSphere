-- V9: Align All Schema Decimals with JPA Entity Double
-- ============================================================================

-- assessments
ALTER TABLE assessments ALTER COLUMN passing_score TYPE DOUBLE PRECISION;

-- assessment_sessions
ALTER TABLE assessment_sessions ALTER COLUMN total_score TYPE DOUBLE PRECISION;
ALTER TABLE assessment_sessions ALTER COLUMN max_possible_score TYPE DOUBLE PRECISION;
ALTER TABLE assessment_sessions ALTER COLUMN proctoring_anomaly_score TYPE DOUBLE PRECISION;

-- session_answers
ALTER TABLE session_answers ALTER COLUMN score TYPE DOUBLE PRECISION;

-- certifications
ALTER TABLE certifications ALTER COLUMN score TYPE DOUBLE PRECISION;

-- skill_scores
ALTER TABLE skill_scores ALTER COLUMN proficiency_score TYPE DOUBLE PRECISION;
