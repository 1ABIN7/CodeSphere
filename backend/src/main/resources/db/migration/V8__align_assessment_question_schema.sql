-- V8: Align Assessment Question Schema with JPA Entity
-- ============================================================================

-- Alter columns from DECIMAL to DOUBLE PRECISION to match Java Double
ALTER TABLE assessment_questions ALTER COLUMN max_score TYPE DOUBLE PRECISION;
ALTER TABLE assessment_questions ALTER COLUMN negative_score TYPE DOUBLE PRECISION;
