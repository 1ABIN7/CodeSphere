-- ============================================================================
-- V5: Judge Engine & File Storage Schema Extensions
-- ============================================================================
-- Extends V4 tables (problems, test_cases, submissions) with additional
-- columns for the AI-powered judge engine and file storage subsystem.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Extend problems table with richer metadata
-- ---------------------------------------------------------------------------
ALTER TABLE problems ADD COLUMN IF NOT EXISTS tags JSONB DEFAULT '[]'::jsonb;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS hints JSONB DEFAULT '[]'::jsonb;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS editorial TEXT;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS editorial_code TEXT;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS is_published BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS acceptance_rate DECIMAL DEFAULT 0.0 NOT NULL;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS total_submissions INT DEFAULT 0 NOT NULL;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS accepted_submissions INT DEFAULT 0 NOT NULL;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS starter_code JSONB DEFAULT '{}'::jsonb;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS company_tags JSONB DEFAULT '[]'::jsonb;

-- ---------------------------------------------------------------------------
-- 2. Extend test_cases table
-- ---------------------------------------------------------------------------
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS explanation TEXT;
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 0 NOT NULL;
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS time_limit_override INT;
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS score_weight INT DEFAULT 1 NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Extend submissions table
-- ---------------------------------------------------------------------------
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS score INT DEFAULT 0;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS test_cases_passed INT DEFAULT 0;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS total_test_cases INT DEFAULT 0;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS ai_feedback JSONB DEFAULT '{}'::jsonb;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS complexity_analysis JSONB DEFAULT '{}'::jsonb;

-- ---------------------------------------------------------------------------
-- 4. Per-test-case execution results
-- ---------------------------------------------------------------------------
CREATE TABLE submission_results (
    id              BIGSERIAL PRIMARY KEY,
    submission_id   BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    test_case_id    BIGINT NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    actual_output   TEXT,
    exec_time       INT,          -- milliseconds
    exec_memory     INT,          -- kilobytes
    error_output    TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_subresults_submission ON submission_results(submission_id);
CREATE INDEX idx_subresults_testcase ON submission_results(test_case_id);

-- ---------------------------------------------------------------------------
-- 5. File attachments (multi-entity polymorphic storage)
-- ---------------------------------------------------------------------------
CREATE TABLE file_attachments (
    id              BIGSERIAL PRIMARY KEY,
    uploader_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    entity_type     VARCHAR(50) NOT NULL,   -- PROBLEM, SUBMISSION, EDITORIAL, PROFILE
    entity_id       BIGINT NOT NULL,
    file_name       VARCHAR(500) NOT NULL,
    file_path       VARCHAR(1000) NOT NULL,
    file_size       BIGINT NOT NULL,        -- bytes
    content_type    VARCHAR(255) NOT NULL,
    storage_backend VARCHAR(20) NOT NULL DEFAULT 'LOCAL',  -- LOCAL, MINIO
    checksum        VARCHAR(128),           -- SHA-256 hex
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_files_entity ON file_attachments(entity_type, entity_id);
CREATE INDEX idx_files_uploader ON file_attachments(uploader_id);

-- ---------------------------------------------------------------------------
-- 6. Problem tags lookup for fast filtering
-- ---------------------------------------------------------------------------
CREATE TABLE problem_tags (
    id          BIGSERIAL PRIMARY KEY,
    problem_id  BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag         VARCHAR(100) NOT NULL
);

CREATE INDEX idx_problem_tags_tag ON problem_tags(tag);
CREATE INDEX idx_problem_tags_problem ON problem_tags(problem_id);

-- ---------------------------------------------------------------------------
-- 7. Additional indexes for performance
-- ---------------------------------------------------------------------------
CREATE INDEX idx_problems_difficulty ON problems(difficulty);
CREATE INDEX idx_problems_published ON problems(is_published);
CREATE INDEX idx_submissions_status ON submissions(status);
CREATE INDEX idx_submissions_created ON submissions(created_at DESC);
