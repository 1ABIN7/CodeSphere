# CodeSphere — Implementation Status

> **Project:** Online Assessment Platform — Caytm Technologies Internship  
> **Stack:** Java 21 + Spring Boot 3.3.1 · PostgreSQL 16 · React + Vite (planned) · Docker · Nginx  
> **Last Updated:** July 2026

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Repository Structure](#2-repository-structure)
3. [Tech Stack & Dependencies](#3-tech-stack--dependencies)
4. [Database Schema (Flyway Migrations)](#4-database-schema-flyway-migrations)
5. [Backend — Domain Models](#5-backend--domain-models)
6. [Backend — Repositories](#6-backend--repositories)
7. [Backend — DTOs](#7-backend--dtos)
8. [Backend — Services](#8-backend--services)
9. [Backend — Controllers (API Endpoints)](#9-backend--controllers-api-endpoints)
10. [Backend — Security Layer](#10-backend--security-layer)
11. [Backend — Configuration](#11-backend--configuration)
12. [Backend — Data Seeder](#12-backend--data-seeder)
13. [Testing](#13-testing)
14. [DevOps & Infrastructure](#14-devops--infrastructure)
15. [Documentation](#15-documentation)
16. [What's NOT Yet Implemented](#16-whats-not-yet-implemented)

---

## 1. Project Overview

CodeSphere is a multi-tenant **Online Assessment Platform** supporting:
- Multiple question types: Coding, MCQ (single/multi), Subjective, Reading Comprehension, File Upload
- Exam/assessment management with time limits, sections, shuffling, and access codes
- Proctoring event tracking
- Code submission judging (problem bank + test cases)
- Certification issuance on passing scores
- Skill scoring per candidate
- Role-based access for Super Admins, Org Admins, Examiners, Instructors, and Candidates

---

## 2. Repository Structure

```
CodeSphere/
├── .env.example                    # Environment variable template
├── .gitignore
├── Dockerfile.backend              # Multi-stage Docker build for backend
├── Dockerfile.frontend             # Docker build for frontend (placeholder)
├── nginx.conf                      # Nginx reverse-proxy config
├── README.md                       # Project quickstart guide
├── WEEK_1_REPORT.md                # Week 1 progress report
│
├── .github/
│   └── workflows/
│       └── ci.yml                  # GitHub Actions CI pipeline
│
├── backend/                        # Spring Boot application root
│   ├── pom.xml                     # Maven build file
│   ├── mvnw / mvnw.cmd             # Maven wrapper
│   │
│   ├── controller/
│   │   └── AuthController.java
│   ├── dto/
│   │   ├── AuthResponse.java
│   │   ├── ForgotPasswordRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   └── ResetPasswordRequest.java
│   ├── model/
│   │   ├── AuditLog.java
│   │   ├── Organization.java       # (file contains OrganizationService — naming mismatch)
│   │   ├── RefreshToken.java
│   │   ├── Role.java               # Enum
│   │   └── User.java
│   ├── repository/
│   │   ├── AusitLogRepository.java
│   │   ├── RefreshTokenRepository.java
│   │   └── UserRepository.java
│   ├── Security/
│   │   ├── JwtTokenProvider.java
│   │   └── SecurityConfig.java
│   ├── service/
│   │   ├── AuditLogService.java
│   │   ├── AuthService.java
│   │   ├── AuthServiceTest.java    # Unit tests (placed in service/ -- see Testing section)
│   │   ├── CustomerDetailService.java
│   │   └── RefreshTokenService.java
│   │
│   └── src/main/
│       ├── java/com/CodeSphere/backend/
│       │   ├── BackendApplication.java
│       │   ├── config/
│       │   │   ├── OpenApiConfig.java
│       │   │   └── SecurityConfig.java
│       │   └── seeder/
│       │       └── DataSeeder.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/
│               ├── V1__init_schema.sql
│               ├── V2__question_assessment_schema.sql
│               ├── V3__session_evaluation_schema.sql
│               └── V4__problems_contests_certs.sql
│
└── docs/
    ├── api/                        # (placeholder -- .gitkeep only)
    ├── architecture/README.md
    ├── database/README.md          # Full schema reference
    ├── deployment/README.md        # Setup & environment guide
    └── testing/                    # (placeholder -- .gitkeep only)
```

> NOTE: The `backend/` folder has **two parallel structures** — files at the root of `backend/` (likely early-stage work) and the canonical Spring Boot structure under `src/main/java/`. Both are active and the codebase has some duplication/inconsistency between them.

---

## 3. Tech Stack & Dependencies

### Backend (pom.xml)

| Dependency | Version | Purpose |
|---|---|---|
| Spring Boot | 3.3.1 | Core framework |
| Java | 21 | Runtime |
| Spring Security | (Boot-managed) | Authentication & authorization |
| Spring Data JPA | (Boot-managed) | ORM / database access |
| Spring Web | (Boot-managed) | REST API layer |
| Spring Validation | (Boot-managed) | Bean validation (@Valid, @NotBlank, etc.) |
| PostgreSQL Driver | (Boot-managed) | Production database connectivity |
| Flyway Core + PostgreSQL | (Boot-managed) | Database migration management |
| JJWT (jjwt-api/impl/jackson) | 0.11.5 | JWT token generation & validation |
| Lombok | 1.18.40 | Boilerplate reduction (@Getter, @Builder, etc.) |
| MapStruct | 1.5.5.Final | DTO to Entity mapping (configured, not yet actively used) |
| H2 (test scope) | (Boot-managed) | In-memory database for unit tests |
| Spring Security Test | (test scope) | Security context mocking in tests |

### Frontend
- **Planned:** React + Vite (no frontend code exists yet)

### Infrastructure
- **Docker** — Multi-stage builds for backend (and frontend placeholder)
- **Nginx** — Reverse proxy routing frontend `/` and backend `/api/`
- **GitHub Actions** — CI pipeline on push to `develop` / PRs to `main`

---

## 4. Database Schema (Flyway Migrations)

All migrations are in `src/main/resources/db/migration/` and run automatically on startup.

### V1 — Core Identity Schema

| Table | Description |
|---|---|
| `organizations` | Multi-tenant container; id, name (unique), timestamps |
| `users` | User accounts; email/password, organization_id FK, enabled flag |
| `roles` | Role definitions (ROLE_USER, ROLE_ADMIN, ROLE_ORGANIZATION_ADMIN) |
| `user_roles` | Many-to-many join table between users and roles |
| `audit_logs` | Action tracking; user_id, action, resource_type, resource_id, ip_address, user_agent |
| `refresh_tokens` | JWT refresh token storage; token (unique), expiry_date, revoked flag |

**Seeded Roles:** ROLE_USER, ROLE_ADMIN, ROLE_ORGANIZATION_ADMIN

---

### V2 — Question Bank & Assessments

| Table | Description |
|---|---|
| `question_categories` | Hierarchical categories (self-referencing parent_id) per organization |
| `question_bank` | Multi-type questions: CODING, MCQ_SINGLE, MCQ_MULTI, SUBJECTIVE, READING_COMPREHENSION, FILE_UPLOAD. Stores options (JSONB), correct_answer, difficulty, tags (JSONB), passage_text, approval status and version |
| `question_versions` | Snapshot history of question edits (content_snapshot JSONB) |
| `rubrics` | Scoring rubric criteria (criteria JSONB) per question |
| `assessments` | Full exam definition: type, duration, start/end time, passing score, shuffle flags, certifying flag, published flag, optional access_code |
| `assessment_sections` | Ordered sections within an assessment; supports SEQUENTIAL or FREE navigation |
| `assessment_questions` | Links questions to assessments/sections with order_index, max_score, negative_score |
| `assessment_assignments` | Assigns specific assessments to specific users with optional deadline |

---

### V3 — Session & Evaluation

| Table | Description |
|---|---|
| `assessment_sessions` | Candidate exam attempt; status: NOT_STARTED, IN_PROGRESS, SUBMITTED, GRADING, GRADED, DISQUALIFIED. Stores proctoring_anomaly_score |
| `session_answers` | Per-question answer record; supports answer_text, file_url, selected_options (JSONB), rubric_scores (JSONB); tracks auto vs manual grading |
| `evaluation_reviews` | Manual reviewer scores/feedback per answer |
| `proctoring_events` | Proctoring flag events: TAB_SWITCH, FACE_NOT_FOUND, VOICE_DETECTED etc. with INFO/WARNING/CRITICAL severity, optional screenshot_url |

**Indexes:** sessions(user_id), sessions(assessment_id), answers(session_id), proctoring_events(session_id)

---

### V4 — Problems, Contests & Certifications

| Table | Description |
|---|---|
| `problems` | Coding problems with description, input_format, output_format, constraints, difficulty, time_limit (ms), memory_limit (KB) |
| `test_cases` | Input/expected-output pairs per problem; is_sample flag |
| `submissions` | Code submissions: language, code, status (PENDING/ACCEPTED/WRONG_ANSWER/TLE/MLE/CE/RE), exec_time, exec_memory, error_message |
| `certifications` | Issued certificates: cert_title, issued_by, verification_code (unique), linked to exam_session_id, score, is_valid |
| `skill_scores` | Per-user, per-skill proficiency tracking; proficiency_score, problems_solved — unique on (user_id, skill_category) |

**Indexes:** submissions(user_id, problem_id), certifications(user_id)

---

## 5. Backend — Domain Models

### Role (Enum)
Defined in `backend/model/Role.java`:
- ROLE_SUPER_ADMIN
- ROLE_ORG_ADMIN
- ROLE_EXAMINER
- ROLE_INSTRUCTOR
- ROLE_CANDIDATE

> NOTE: The SQL V1 migration seeds ROLE_USER, ROLE_ADMIN, ROLE_ORGANIZATION_ADMIN — a mismatch with the Java enum that needs reconciliation.

---

### User Entity
- Fields: id, username (unique), email (unique), password (BCrypt), role (enum), organization (ManyToOne lazy), createdAt
- Auth fields: resetPasswordToken, resetPasswordTokenExpiry (Instant), emailVerified (boolean), emailVerificationToken
- @PrePersist sets createdAt automatically

> BUG: `organization` field is declared **twice** in User.java.

---

### Organization (model entity)
- Schema: id, name (unique), timestamps

### OrganizationService (in wrong file)
*(Note: `backend/model/Organization.java` actually contains OrganizationService code — a filename/content mismatch)*
- Service provides full CRUD + member management (addMember, removeMember, getMembers)

---

### RefreshToken Entity
- Fields: id, user (OneToOne FK), token (unique), expiryDate
- Stored in refresh_tokens table

---

### AuditLog Entity
- Fields: id, userId, action, resource, timestamp, ipAddress
- Used for admin-level activity tracking

---

## 6. Backend — Repositories

| Repository | Extends | Custom Methods |
|---|---|---|
| UserRepository | JpaRepository<User, Long> | findByUsername, findByEmail, existsByUsername, existsByEmail, findByResetPasswordToken, findByEmailVerificationToken |
| RefreshTokenRepository | JpaRepository<RefreshToken, Long> | findByToken, deleteByUser (@Modifying) |
| AuditLogRepository (file named AusitLogRepository.java) | JpaRepository<AuditLog, Long> | (standard CRUD) |

---

## 7. Backend — DTOs

| DTO | Fields | Validation |
|---|---|---|
| RegisterRequest | username, email, password | @NotBlank, @Email, @Size(min=3,max=20) on username, @Size(min=6) on password |
| LoginRequest | usernameOrEmail, password | @NotBlank on both |
| AuthResponse | token, refreshToken, username, role | (response only) |
| ForgotPasswordRequest | email | none declared |
| ResetPasswordRequest | token, newPassword | none declared |

---

## 8. Backend — Services

### AuthService
File: `backend/service/AuthService.java`

| Method | Description |
|---|---|
| register(RegisterRequest) | Checks uniqueness of username/email, encodes password with BCrypt, assigns ROLE_CANDIDATE, generates email verification token, saves user. Prints verification link to console (no email sender yet). |
| verifyEmail(String token) | Looks up user by verification token, sets emailVerified=true, clears token. |
| login(LoginRequest) | Finds user by username or email, validates password, creates Spring Authentication, generates RS256 JWT access token + UUID refresh token, returns AuthResponse. |
| logout() | Client-side only (clears Security context in controller). |
| processForgotPassword(ForgotPasswordRequest) | Generates UUID reset token, sets 15-min expiry, saves. Prints token to console (no email sender yet). |
| processResetPassword(ResetPasswordRequest) | Validates token + expiry, encodes new password, clears token fields. |

---

### RefreshTokenService
File: `backend/service/RefreshTokenService.java`

| Method | Description |
|---|---|
| createRefreshToken(Long userId) | Creates a UUID-based refresh token with 7-day expiry, saves to DB. |
| verifyExpiration(RefreshToken) | Deletes and throws if expired; returns token if valid. |
| deleteByUserId(Long userId) | Deletes all refresh tokens for a user (transactional). |

---

### AuditLogService
File: `backend/service/AuditLogService.java`

| Method | Description |
|---|---|
| logAction(Long userId, String action, String resource, HttpServletRequest) | Extracts IP (handles X-Forwarded-For proxy headers), saves AuditLog record. |

---

### CustomUserDetailsService
File: `backend/service/CustomerDetailService.java` (package: com.CodeSphere.backend.security)

- Implements Spring Security's UserDetailsService
- loadUserByUsername: Accepts username OR email, loads User, converts to Spring Security UserDetails with a single SimpleGrantedAuthority from user.getRole().name()

---

### OrganizationService
File: `backend/model/Organization.java` (misnamed file)

| Method | Description |
|---|---|
| createOrganization(OrganizationRequest) | Checks name uniqueness, builds and saves org. |
| getAllOrganizations() | Returns all organizations. |
| getOrganizationById(Long) | Finds by ID or throws. |
| updateOrganization(Long, OrganizationRequest) | Updates org name. |
| deleteOrganization(Long) | Detaches all member users before deleting org. |
| addMember(Long orgId, Long userId) | Sets user's organization FK. |
| removeMember(Long orgId, Long userId) | Validates membership, clears user's organization FK. |
| getMembers(Long orgId) | Returns Set<User> of org members. |

---

## 9. Backend — Controllers (API Endpoints)

### AuthController
Base path: `/api/auth`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | /api/auth/register | Register new user (defaults to ROLE_CANDIDATE) | No - Public |
| POST | /api/auth/login | Authenticate with username/email + password; returns JWT + refresh token | No - Public |
| POST | /api/auth/logout | Clears server-side security context | Yes - Authenticated |
| POST | /api/auth/forgot-password | Request password reset token | No - Public |
| POST | /api/auth/reset-password | Submit new password using reset token | No - Public |
| GET | /api/auth/verify-email?token= | Verify email address via token link | No - Public |

> No other controllers (Organization, Problem, Assessment, etc.) have been implemented yet.

---

## 10. Backend — Security Layer

### JwtTokenProvider
File: `backend/Security/JwtTokenProvider.java`

- **Algorithm:** RS256 (RSA 2048-bit asymmetric key pair)
- Keys are **generated in-memory at startup** (new pair per restart — not persistent; fine for dev, needs key management for prod)
- Token expiry: **24 hours** (hardcoded; base application.yml specifies 15 min — mismatch)

| Method | Description |
|---|---|
| generateToken(Authentication) | Signs JWT with private key, sets subject = username |
| getUsernameFromJWT(String) | Extracts username claim using public key |
| validateToken(String) | Parses and validates signature/expiry; returns boolean |

---

### SecurityConfig — Two Versions

#### Version 1 — backend/Security/SecurityConfig.java
Role-based route rules:
- /api/auth/** — public
- /api/super-admin/** — ROLE_SUPER_ADMIN only
- /api/org/** — SUPER_ADMIN or ORG_ADMIN
- /api/exams/manage/** — SUPER_ADMIN, ORG_ADMIN, or EXAMINER
- /api/courses/** — SUPER_ADMIN, ORG_ADMIN, or INSTRUCTOR
- /api/candidate/** — ROLE_CANDIDATE only
- All others — authenticated

#### Version 2 — src/main/java/.../config/SecurityConfig.java
Simpler, with CORS configured:
- /auth/** — public
- GET /problems/** — public
- /swagger-ui/**, /v3/api-docs/** — public
- All others — authenticated
- CORS: allows http://localhost:5173 and http://localhost:3000

> WARNING: Two competing SecurityConfig classes exist — this will cause a Spring bean conflict and needs consolidation.

---

## 11. Backend — Configuration

### Application Profiles

| Profile | File | Key Settings |
|---|---|---|
| Default | application.yml | Port 8080, Hibernate validate, Flyway enabled, OSIV off, JWT secret (base64), access token 15 min, refresh token 7 days |
| Development | application-dev.yml | PostgreSQL (localhost:5432/codesphere), Redis (localhost:6379), RabbitMQ (localhost:5672), MinIO (localhost:9000), DEBUG logging |
| Production | application-prod.yml | Uses environment variables (presumed) |

### OpenApiConfig
- Swagger UI at: http://localhost:8080/swagger-ui.html
- Raw JSON at: http://localhost:8080/v3/api-docs
- API title: "CodeSphere API v1.0.0"
- Auto-scans all @RestController classes

---

## 12. Backend — Data Seeder

File: `src/main/java/.../seeder/DataSeeder.java`  
Active profile: dev only (@Profile("dev"))

Seeds the following on first startup (skips if users already exist):

| Seed | Data |
|---|---|
| Organization | "Demo Corp" |
| Users | admin@demo.com (ROLE_ADMIN), evaluator@demo.com (ROLE_USER), candidate@demo.com (ROLE_USER) — all password: password123 |
| Questions (5) | MCQ_SINGLE and MCQ_MULTI questions about algorithms, Java, HTTP, REST, OOP |
| Assessment | "Demo MCQ Assessment" — 30 min, 60% passing, published, MCQ type |

---

## 13. Testing

### Unit Tests — AuthServiceTest
File: `backend/service/AuthServiceTest.java`  
NOTE: Placed in service/ folder instead of src/test/ — will NOT be picked up by Maven Surefire automatically.

Uses Mockito + JUnit 5:

| Test | Covers |
|---|---|
| register_Success | Happy path registration — verifies BCrypt encoding, ROLE_CANDIDATE assignment, email verification token set |
| register_ThrowsException_WhenUsernameExists | Duplicate username guard |
| processForgotPassword_Success | Reset token generation and expiry set |
| processResetPassword_Success | Password update, token cleared after valid reset |
| processResetPassword_ThrowsException_WhenTokenExpired | Expired token rejection |
| verifyEmail_Success | Email verification token consumption |

---

## 14. DevOps & Infrastructure

### GitHub Actions CI (.github/workflows/ci.yml)
- **Triggers:** Push to develop, PRs to main or develop
- **Steps:** Checkout -> Setup JDK 21 (Temurin) -> mvn clean install -DskipTests -> mvn test

### Dockerfile (Backend) — Multi-Stage
- Stage 1 (build): maven:3.9.6-eclipse-temurin-21 -> mvn clean package -DskipTests
- Stage 2 (run): eclipse-temurin:21-jre-alpine -> java -jar app.jar (port 8080)

### Nginx (nginx.conf)
| Route | Proxied To |
|---|---|
| / | http://frontend:80 (React SPA) |
| /api/ | http://backend:8080/ |
| /ws | http://backend:8080 (WebSocket upgrade supported) |

### Branch Strategy
- main -> stable production only
- develop -> active development
- Never push directly to main; raise PR from develop

---

## 15. Documentation

| Document | Location | Status |
|---|---|---|
| Project README | README.md | Complete |
| Week 1 Report | WEEK_1_REPORT.md | Complete |
| Database Schema Reference | docs/database/README.md | Complete (all 4 migrations documented) |
| Deployment Guide | docs/deployment/README.md | Complete |
| Architecture Overview | docs/architecture/README.md | Stub (2 lines only) |
| API Documentation | docs/api/ | Empty (placeholder only) |
| Testing Documentation | docs/testing/ | Empty (placeholder only) |
| OpenAPI/Swagger | Auto-generated at /swagger-ui.html | Configured |

---

## 16. What's NOT Yet Implemented

### Frontend
- No React/Vite frontend code exists at all

### Backend Controllers (no endpoints for these yet)
- OrganizationController — CRUD for organizations + member management
- ProblemController — Problem listing, creation, management
- AssessmentController — Assessment CRUD, publishing, assignment
- SessionController — Candidate exam start/submit/resume
- QuestionController — Question bank CRUD
- SubmissionController — Code submission + judging
- CertificationController — Certificate issuance + verification
- AdminController — Super admin panel endpoints
- ProctoringController — Proctoring event reporting

### Backend Services (entities exist in DB but no service layer)
- QuestionService — CRUD for question bank, categories, rubrics
- AssessmentService — Exam management, publishing, assignments
- SessionService — Start/submit/resume exam sessions
- EvaluationService — Manual grading, rubric scoring
- ProblemService — Coding problem management
- SubmissionService — Code judging pipeline
- CertificationService — Certificate issuance + verification code generation
- SkillScoreService — Proficiency tracking

### Infrastructure
- JWT filter not wired into Spring Security filter chain (no JwtAuthenticationFilter)
- No Docker Compose file (docker-compose.yml) — only individual Dockerfiles
- No Redis integration (configured in YAML but no @RedisRepository or cache usage)
- No RabbitMQ consumers/producers (configured in YAML but no messaging code)
- No MinIO integration (configured in YAML but no file upload service)
- No email service (reset token and verification token only print to console)
- No rate limiting

### Known Issues / Technical Debt
- AuthServiceTest.java is in backend/service/ not src/test/ — Maven won't pick it up
- backend/model/Organization.java contains OrganizationService code (wrong file)
- User.java has duplicate organization field declaration
- Two SecurityConfig classes will cause a Spring bean conflict
- JWT expiry: JwtTokenProvider hardcodes 24h; application.yml sets 15 min — inconsistent
- Role enum (ROLE_CANDIDATE, ROLE_SUPER_ADMIN etc.) doesn't match seeded SQL roles (ROLE_USER, ROLE_ADMIN)
- No @PasswordEncoder bean in canonical SecurityConfig (in src/main/); only declared in backend/Security/SecurityConfig.java

---

*Document auto-generated by reviewing all source files — July 2026*
