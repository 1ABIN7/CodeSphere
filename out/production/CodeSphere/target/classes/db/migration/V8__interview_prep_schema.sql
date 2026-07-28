-- ============================================================================
-- V8: Interview Preparation Module
-- ============================================================================
-- Adds tables for a comprehensive interview prep system covering:
-- Technical, Aptitude, Logical Reasoning, Grammar/Verbal, Behavioral/HR,
-- Situational, and Case Study question types.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Interview categories (top-level grouping)
-- ---------------------------------------------------------------------------
CREATE TABLE interview_categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,   -- e.g. TECHNICAL, APTITUDE
    display_name    VARCHAR(200) NOT NULL,
    description     TEXT,
    icon            VARCHAR(100),                   -- e.g. emoji or icon class
    color           VARCHAR(30),                    -- hex color for UI
    is_active       BOOLEAN DEFAULT TRUE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ---------------------------------------------------------------------------
-- 2. Interview question bank
-- ---------------------------------------------------------------------------
CREATE TABLE interview_questions (
    id                  BIGSERIAL PRIMARY KEY,
    category_id         BIGINT NOT NULL REFERENCES interview_categories(id) ON DELETE CASCADE,
    question_type       VARCHAR(50) NOT NULL,       -- MCQ, FILL_BLANK, TRUE_FALSE, SHORT_ANSWER, CODING, CASE_STUDY
    difficulty          VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- EASY, MEDIUM, HARD
    topic               VARCHAR(200),               -- sub-topic within category
    question_text       TEXT NOT NULL,
    options             JSONB DEFAULT '[]'::jsonb,  -- [{id, text}] for MCQ / TRUE_FALSE
    correct_answer      TEXT NOT NULL,              -- For MCQ: option id; for others: answer text
    explanation         TEXT,                       -- why the answer is correct
    tags                JSONB DEFAULT '[]'::jsonb,  -- keyword tags for filtering
    time_limit_seconds  INT DEFAULT 120,
    hints               JSONB DEFAULT '[]'::jsonb,  -- optional hints
    company_tags        JSONB DEFAULT '[]'::jsonb,  -- companies known to ask this
    created_by          BIGINT REFERENCES users(id) ON DELETE SET NULL,
    is_active           BOOLEAN DEFAULT TRUE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_iq_category    ON interview_questions(category_id);
CREATE INDEX idx_iq_difficulty  ON interview_questions(difficulty);
CREATE INDEX idx_iq_type        ON interview_questions(question_type);
CREATE INDEX idx_iq_topic       ON interview_questions(topic);
CREATE INDEX idx_iq_active      ON interview_questions(is_active);

-- ---------------------------------------------------------------------------
-- 3. Interview practice sessions
-- ---------------------------------------------------------------------------
CREATE TABLE interview_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_type        VARCHAR(50) NOT NULL DEFAULT 'PRACTICE', -- PRACTICE, MOCK_INTERVIEW, TIMED_TEST
    category_ids        JSONB DEFAULT '[]'::jsonb,
    total_questions     INT NOT NULL DEFAULT 10,
    time_limit_minutes  INT DEFAULT NULL,           -- NULL means untimed
    score               INT DEFAULT 0,
    max_score           INT DEFAULT 0,
    questions_answered  INT DEFAULT 0,
    correct_answers     INT DEFAULT 0,
    status              VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, ABANDONED
    started_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at        TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_isess_user   ON interview_sessions(user_id);
CREATE INDEX idx_isess_status ON interview_sessions(status);

-- ---------------------------------------------------------------------------
-- 4. Session-question junction (which questions are in this session)
-- ---------------------------------------------------------------------------
CREATE TABLE interview_session_questions (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE,
    order_index INT NOT NULL DEFAULT 0,
    is_answered BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_isq_session ON interview_session_questions(session_id);

-- ---------------------------------------------------------------------------
-- 5. Per-question attempts within a session
-- ---------------------------------------------------------------------------
CREATE TABLE interview_attempts (
    id                    BIGSERIAL PRIMARY KEY,
    session_id            BIGINT NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_id           BIGINT NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE,
    user_answer           TEXT,
    is_correct            BOOLEAN DEFAULT FALSE,
    time_taken_seconds    INT DEFAULT 0,
    score                 INT DEFAULT 0,
    attempted_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_iattempt_session  ON interview_attempts(session_id);
CREATE INDEX idx_iattempt_question ON interview_attempts(question_id);

-- ---------------------------------------------------------------------------
-- 6. User performance tracking per category
-- ---------------------------------------------------------------------------
CREATE TABLE interview_performance (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id         BIGINT NOT NULL REFERENCES interview_categories(id) ON DELETE CASCADE,
    total_attempted     INT DEFAULT 0,
    correct_count       INT DEFAULT 0,
    total_sessions      INT DEFAULT 0,
    best_score          INT DEFAULT 0,
    avg_time_seconds    DECIMAL DEFAULT 0.0,
    last_attempted_at   TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    UNIQUE (user_id, category_id)
);

CREATE INDEX idx_iperf_user ON interview_performance(user_id);

-- ---------------------------------------------------------------------------
-- 7. Seed initial interview categories
-- ---------------------------------------------------------------------------
INSERT INTO interview_categories (name, display_name, description, icon, color) VALUES
('TECHNICAL',    'Technical',              'Core CS concepts, DSA, system design, programming languages', '💻', '#6366f1'),
('APTITUDE',     'Quantitative Aptitude',  'Number series, percentages, time & work, ratios, profit & loss', '🔢', '#10b981'),
('LOGICAL',      'Logical Reasoning',      'Syllogisms, blood relations, pattern recognition, seating arrangements', '🧩', '#f59e0b'),
('GRAMMAR',      'Verbal & Grammar',       'Fill in the blanks, synonyms/antonyms, error correction, comprehension', '📝', '#ec4899'),
('BEHAVIORAL',   'Behavioral / HR',        'Situational questions, leadership, teamwork, conflict resolution', '🤝', '#8b5cf6'),
('CASE_STUDY',   'Case Study',             'Business scenarios, system design mini-cases, problem-solving', '📊', '#06b6d4');
