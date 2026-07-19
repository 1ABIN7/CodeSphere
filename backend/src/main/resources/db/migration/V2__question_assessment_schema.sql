-- Create question_categories table
CREATE TABLE question_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT,
    description TEXT,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES question_categories(id) ON DELETE SET NULL
);

-- Create question_bank table
CREATE TABLE question_bank (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    question_type VARCHAR(50) NOT NULL, -- CODING, MCQ_SINGLE, MCQ_MULTI, SUBJECTIVE, READING_COMPREHENSION, FILE_UPLOAD
    content TEXT NOT NULL,
    options JSONB, -- MCQ options list
    correct_answer TEXT, -- correct options/answer key
    difficulty VARCHAR(50) DEFAULT 'MEDIUM' NOT NULL, -- EASY, MEDIUM, HARD
    category_id BIGINT REFERENCES question_categories(id) ON DELETE SET NULL,
    tags JSONB, -- tags array
    passage_text TEXT, -- For READING_COMPREHENSION passages
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    is_approved BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 1 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create question_versions table
CREATE TABLE question_versions (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES question_bank(id) ON DELETE CASCADE,
    content_snapshot JSONB NOT NULL,
    version INT NOT NULL,
    changed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create rubrics table
CREATE TABLE rubrics (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES question_bank(id) ON DELETE CASCADE,
    criteria JSONB NOT NULL -- [{name, max_points, description}]
);

-- Create assessments table
CREATE TABLE assessments (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assessment_type VARCHAR(50) NOT NULL, -- CODING, MCQ, WRITTEN, READING, FILE_UPLOAD, MIXED
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    duration_minutes INT,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    passing_score DECIMAL,
    instructions TEXT,
    shuffle_questions BOOLEAN DEFAULT FALSE NOT NULL,
    shuffle_options BOOLEAN DEFAULT FALSE NOT NULL,
    allow_resume BOOLEAN DEFAULT TRUE NOT NULL,
    is_certifying BOOLEAN DEFAULT FALSE NOT NULL,
    is_published BOOLEAN DEFAULT FALSE NOT NULL,
    access_code VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create assessment_sections table
CREATE TABLE assessment_sections (
    id BIGSERIAL PRIMARY KEY,
    assessment_id BIGINT NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    section_order INT NOT NULL,
    time_limit_minutes INT,
    section_type VARCHAR(100),
    navigation_mode VARCHAR(50) DEFAULT 'FREE' NOT NULL -- SEQUENTIAL, FREE
);

-- Create assessment_questions table
CREATE TABLE assessment_questions (
    id BIGSERIAL PRIMARY KEY,
    assessment_id BIGINT NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    section_id BIGINT REFERENCES assessment_sections(id) ON DELETE SET NULL,
    question_bank_id BIGINT NOT NULL REFERENCES question_bank(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    max_score DECIMAL DEFAULT 1.0 NOT NULL,
    negative_score DECIMAL DEFAULT 0.0 NOT NULL,
    time_limit_override INT -- overridden section/problem timer
);

-- Create assessment_assignments table
CREATE TABLE assessment_assignments (
    id BIGSERIAL PRIMARY KEY,
    assessment_id BIGINT NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deadline TIMESTAMP WITH TIME ZONE
);