CREATE TABLE IF NOT EXISTS assessment_evaluator_assignments (
  id BIGSERIAL PRIMARY KEY,
  answer_id BIGINT NOT NULL,
  evaluator_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT uq_assessment_evaluator_assignment UNIQUE (answer_id, evaluator_id)
);
