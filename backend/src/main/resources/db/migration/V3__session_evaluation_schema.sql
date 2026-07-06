-- Create assessment_sessions table
CREATE TABLE assessment_sessions (
    id BIGSERIAL PRIMARY KEY,
    assessment_id BIGINT NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'NOT_STARTED' NOT NULL, -- NOT_STARTED, IN_PROGRESS, SUBMITTED, GRADING, GRADED, DISQUALIFIED
    started_at TIMESTAMP WITH TIME ZONE,
    submitted_at TIMESTAMP WITH TIME ZONE,
    last_active_at TIMESTAMP WITH TIME ZONE,
    total_score DECIMAL,
    max_possible_score DECIMAL,
    current_section_index INT DEFAULT 0,
    proctoring_anomaly_score DECIMAL DEFAULT 0.0,
    disqualification_reason TEXT
);

-- Create session_answers table
CREATE TABLE session_answers (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES assessment_sessions(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES question_bank(id) ON DELETE CASCADE,
    answer_text TEXT,
    file_url VARCHAR(512),
    selected_options JSONB,
    score DECIMAL,
    evaluator_feedback TEXT,
    evaluated_by BIGINT REFERENCES users(id) ON DELETE SET NULL, -- Maps directly to the users table BIGINT ID
    evaluated_at TIMESTAMP WITH TIME ZONE,
    is_auto_graded BOOLEAN DEFAULT TRUE NOT NULL,
    rubric_scores JSONB
);

-- Create evaluation_reviews table
CREATE TABLE evaluation_reviews (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT NOT NULL REFERENCES session_answers(id) ON DELETE CASCADE,
    evaluator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    scores JSONB,
    feedback TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create proctoring_events table
CREATE TABLE proctoring_events (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES assessment_sessions(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL, -- TAB_SWITCH, FACE_NOT_FOUND, VOICE_DETECTED, etc.
    severity VARCHAR(50) DEFAULT 'INFO' NOT NULL, -- INFO, WARNING, CRITICAL
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    screenshot_url VARCHAR(512),
    metadata JSONB
);

-- Optimization Indexes
CREATE INDEX idx_sessions_user ON assessment_sessions(user_id);
CREATE INDEX idx_sessions_assessment ON assessment_sessions(assessment_id);
CREATE INDEX idx_answers_session ON session_answers(session_id);
CREATE INDEX idx_proctoring_session ON proctoring_events(session_id);
