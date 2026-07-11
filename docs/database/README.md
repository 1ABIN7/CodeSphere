# CodeSphere — Database Schema Documentation

## Overview

CodeSphere uses **PostgreSQL 16** as the primary database.
Schema is managed via **Flyway migrations** — versioned SQL files
that run automatically on application startup.

## Migration History

| Version | File | Description |
|---------|------|-------------|
| V1 | V1__init_schema.sql | Users, roles, organizations, audit logs, refresh tokens |
| V2 | V2__question_assessment_schema.sql | Question bank, assessments, sections, assignments |
| V3 | V3__session_evaluation_schema.sql | Sessions, answers, evaluation reviews, proctoring |
| V4 | V4__problems_contests_certs.sql | Problems, test cases, submissions, certifications |

---

## V1 — Core Tables

### organizations
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| name | VARCHAR(255) | Unique organization name |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Last update time |

### users
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| email | VARCHAR(255) | Unique user email |
| password_hash | VARCHAR(255) | BCrypt hashed password |
| first_name | VARCHAR(100) | First name |
| last_name | VARCHAR(100) | Last name |
| organization_id | BIGINT | FK → organizations |
| enabled | BOOLEAN | Account active status |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Last update time |

### roles
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| name | VARCHAR(50) | Role name (ROLE_USER, ROLE_ADMIN, ROLE_ORGANIZATION_ADMIN) |

### user_roles
| Column | Type | Description |
|--------|------|-------------|
| user_id | BIGINT | FK → users |
| role_id | BIGINT | FK → roles |

### audit_logs
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| user_id | BIGINT | FK → users |
| action | VARCHAR(255) | Action performed |
| resource_type | VARCHAR(100) | Type of resource affected |
| resource_id | VARCHAR(100) | ID of resource affected |
| ip_address | VARCHAR(45) | Client IP address |
| user_agent | TEXT | Client user agent |
| occurred_at | TIMESTAMP | When action occurred |

### refresh_tokens
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| user_id | BIGINT | FK → users |
| token | VARCHAR(255) | Unique refresh token |
| expiry_date | TIMESTAMP | Token expiry time |
| revoked | BOOLEAN | Whether token is revoked |
| created_at | TIMESTAMP | Record creation time |

---

## V2 — Question Bank & Assessments

### question_categories
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| name | VARCHAR(255) | Category name |
| parent_id | BIGINT | FK → question_categories (hierarchical) |
| description | TEXT | Category description |
| organization_id | BIGINT | FK → organizations |

### question_bank
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| title | VARCHAR(255) | Question title |
| question_type | VARCHAR(50) | CODING, MCQ_SINGLE, MCQ_MULTI, SUBJECTIVE, READING_COMPREHENSION, FILE_UPLOAD |
| content | TEXT | Question content |
| options | JSONB | MCQ options array |
| correct_answer | TEXT | Correct answer key |
| difficulty | VARCHAR(50) | EASY, MEDIUM, HARD |
| category_id | BIGINT | FK → question_categories |
| tags | JSONB | Tags array |
| passage_text | TEXT | Passage for reading comprehension |
| created_by | BIGINT | FK → users |
| organization_id | BIGINT | FK → organizations |
| is_approved | BOOLEAN | Approval status |
| version | INT | Version number |
| created_at | TIMESTAMP | Record creation time |

### assessments
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| title | VARCHAR(255) | Assessment title |
| assessment_type | VARCHAR(50) | CODING, MCQ, WRITTEN, READING, FILE_UPLOAD, MIXED |
| organization_id | BIGINT | FK → organizations |
| created_by | BIGINT | FK → users |
| duration_minutes | INT | Time limit in minutes |
| passing_score | DECIMAL | Minimum passing score |
| shuffle_questions | BOOLEAN | Randomize question order |
| shuffle_options | BOOLEAN | Randomize MCQ options |
| is_certifying | BOOLEAN | Issues certificate on pass |
| is_published | BOOLEAN | Published and accessible |
| access_code | VARCHAR(100) | Optional access code |

---

## V3 — Sessions & Evaluation

### assessment_sessions
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| assessment_id | BIGINT | FK → assessments |
| user_id | BIGINT | FK → users |
| status | VARCHAR(50) | NOT_STARTED, IN_PROGRESS, SUBMITTED, GRADING, GRADED, DISQUALIFIED |
| started_at | TIMESTAMP | When exam started |
| submitted_at | TIMESTAMP | When exam submitted |
| total_score | DECIMAL | Final score |
| proctoring_anomaly_score | DECIMAL | Anomaly score 0-100 |

### session_answers
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| session_id | BIGINT | FK → assessment_sessions |
| question_id | BIGINT | FK → question_bank |
| answer_text | TEXT | Written answer |
| file_url | VARCHAR(512) | Uploaded file URL |
| selected_options | JSONB | MCQ selected options |
| score | DECIMAL | Score awarded |
| is_auto_graded | BOOLEAN | Auto or manual graded |

### proctoring_events
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| session_id | BIGINT | FK → assessment_sessions |
| event_type | VARCHAR(100) | TAB_SWITCH, FACE_NOT_FOUND, VOICE_DETECTED etc |
| severity | VARCHAR(50) | INFO, WARNING, CRITICAL |
| occurred_at | TIMESTAMP | When event occurred |
| screenshot_url | VARCHAR(512) | Screenshot URL |

---

## V4 — Problems, Submissions & Certifications

### problems
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| title | VARCHAR(255) | Problem title |
| difficulty | VARCHAR(50) | EASY, MEDIUM, HARD |
| time_limit | INT | Time limit in milliseconds |
| memory_limit | INT | Memory limit in KB |
| created_by | BIGINT | FK → users |

### submissions
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| problem_id | BIGINT | FK → problems |
| user_id | BIGINT | FK → users |
| code | TEXT | Submitted code |
| language | VARCHAR(50) | Programming language |
| status | VARCHAR(50) | PENDING, ACCEPTED, WRONG_ANSWER, TLE, MLE, CE, RE |
| exec_time | INT | Execution time in ms |
| exec_memory | INT | Memory used in KB |

### certifications
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| user_id | BIGINT | FK → users |
| cert_title | VARCHAR(255) | Certificate title |
| verification_code | VARCHAR(100) | Unique verification code |
| exam_session_id | BIGINT | FK → assessment_sessions |
| score | DECIMAL | Score achieved |
| is_valid | BOOLEAN | Certificate validity |

---

*Document maintained by P5 — Abin Joseph*
*Last updated: July 2026*