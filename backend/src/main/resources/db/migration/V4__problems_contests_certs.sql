-- Create problems table
CREATE TABLE problems (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    input_format TEXT,
    output_format TEXT,
    constraints TEXT,
    difficulty VARCHAR(50) DEFAULT 'MEDIUM' NOT NULL,
    time_limit INT DEFAULT 1000 NOT NULL, -- in milliseconds
    memory_limit INT DEFAULT 262144 NOT NULL, -- in KB
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create test_cases table
CREATE TABLE test_cases (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_sample BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create submissions table
CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    code TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    exec_time INT,
    exec_memory INT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create certifications table
CREATE TABLE certifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cert_title VARCHAR(255) NOT NULL,
    issued_by VARCHAR(255) NOT NULL,
    verification_code VARCHAR(100) NOT NULL UNIQUE,
    exam_session_id BIGINT REFERENCES assessment_sessions(id) ON DELETE SET NULL,
    score DECIMAL,
    is_valid BOOLEAN DEFAULT TRUE NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create skill_scores table
CREATE TABLE skill_scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_category VARCHAR(100) NOT NULL,
    proficiency_score DECIMAL DEFAULT 0.0 NOT NULL,
    problems_solved INT DEFAULT 0 NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_skill UNIQUE (user_id, skill_category)
);

-- Optimization Indexes
CREATE INDEX idx_submissions_user_prob ON submissions(user_id, problem_id);
CREATE INDEX idx_certs_user ON certifications(user_id);