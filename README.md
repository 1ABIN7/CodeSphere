# CodeSphere

A coding-assessment and interview-preparation platform — think a lightweight
HackerRank/CoderPad hybrid. Candidates solve coding problems and take
multi-format assessments (MCQ, coding, written, reading comprehension, file
upload); examiners create problems, question banks, and assessments;
proctoring and evaluation tooling sit on top.

Built as a BTech final-year team project.

> **Submission documentation:** [API reference](docs/api/README.md),
> [architecture/design](docs/architecture/README.md),
> [database schema](docs/database/README.md), [deployment guide](docs/deployment/README.md),
> [test evidence](docs/testing/README.md), and the [end-to-end demo guide](docs/DEMO_GUIDE.md).

---

## Features

**Live and working:**
- User registration/login with JWT auth + refresh tokens, role-based access (`SUPER_ADMIN`, `ORG_ADMIN`, `EXAMINER`, `CANDIDATE`)
- Coding problems: CRUD, difficulty levels, tags, publish/unpublish, test cases
- Code submission and judging in Java, Python, C++, C, and JavaScript, with AI-assisted code analysis (complexity, code quality, anti-patterns)
- A second, queue-based judging path (RabbitMQ + WebSocket) for async "submit"/"run" flows
- Assessment authoring: create, clone, publish (with validation), assign to candidates, organize into sections
- File upload/storage abstraction (local disk or MinIO/S3-compatible)
- Basic proctoring: event logging, webcam snapshots, session flagging
- Interview-prep practice sessions with categories and performance tracking

**Current assessment workflow:**
- Admins can create question-bank items and coding/debugging tasks, assemble and publish assessments, assign candidates or groups, and release final results.
- Candidates can take timed assessments with autosave, submit coding/SQL/API/file answers, and view released result history.
- Evaluators can review written and file responses using rubric scores and two-reviewer consensus.
- Reports include assessment, candidate, question, difficulty, and elapsed-time metrics.

---

## Tech Stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.3.1, Spring Security (JWT), Spring Data JPA |
| Database | PostgreSQL 16, Flyway migrations |
| Cache / Rate limiting | Redis 7 |
| Async messaging | RabbitMQ 3.13 |
| Object storage | MinIO (S3-compatible) |
| Frontend | React 19, Vite, React Router, Axios, Monaco Editor, Framer Motion |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Containerization | Docker, Docker Compose |
| CI | GitHub Actions |

---

## Getting Started

### Prerequisites
- Docker + Docker Compose
- (For local, non-Docker backend dev) Java 21, Maven
- (For local, non-Docker frontend dev) Node.js 20+

### Quickest path — full stack via Docker Compose
```bash
git clone https://github.com/1ABIN7/CodeSphere.git
cd CodeSphere
cp .env.example .env   # then fill in real secrets — see below
docker compose up --build
```

This starts Postgres, Redis, RabbitMQ (management UI at
`http://localhost:15672`, guest/guest), MinIO (console at
`http://localhost:9001`, minioadmin/minioadmin), the Spring Boot backend
(`http://localhost:8080`), the frontend (`http://localhost:3000`), and an
Nginx reverse proxy (`http://localhost:80`).

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Backend only (local dev, no Docker)
```bash
cd backend
cp ../.env.example .env   # adjust DB_HOST etc. to localhost
./mvnw spring-boot:run
```
Requires a local Postgres on `5432`, or point `DB_HOST`/`DB_PORT` at your own instance.

### Frontend only (local dev, no Docker)
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`, proxying `/api` to `VITE_API_URL` (defaults to `http://localhost:8080`).

The active application is the React/Vite project in `frontend/`.

### Running tests
```bash
cd backend
mvn test
```

---

## Project Structure

```
CodeSphere/
├── backend/                  # Spring Boot API (Maven)
│   └── src/main/java/com/CodeSphere/backend/
│       ├── controller/        # REST endpoints
│       ├── service/            # Business logic
│       ├── model/               # JPA entities
│       ├── repository/           # Spring Data repositories
│       ├── security/              # JWT, filters, auth config
│       ├── dto/                    # Request/response objects
│       └── config/                  # Spring config (CORS, RabbitMQ, WebSocket, etc.)
├── frontend/                 # React + Vite app (built by Docker Compose)
├── src/, public/              # Legacy source material; not used by the active Vite app
├── docs/
│   ├── api/                   # Endpoint reference
│   ├── architecture/           # System design docs
│   ├── database/                 # Schema docs
│   ├── deployment/                # Deploy guide
│   └── testing/                    # Test coverage docs
├── docker-compose.yml
├── Dockerfile.backend
├── Dockerfile.frontend
└── nginx.conf
```

---

## Documentation

- [Submission Deliverables](docs/DELIVERABLES.md)
- [API Reference](docs/api/README.md)
- [Architecture](docs/architecture/README.md)
- [Database Schema](docs/database/README.md)
- [Deployment Guide](docs/deployment/README.md)
- [Testing](docs/testing/README.md)
- [End-to-End Demonstration](docs/DEMO_GUIDE.md)

---

## Team

| Role | Owner | Focus |
|---|---|---|
| P1 | Rahul | Backend — auth, assessments, question bank, security |
| P2 | Harshith | Judge engine, file storage, RabbitMQ, interview prep |
| P3 | Anup | Database migrations, security config, RabbitMQ/WebSocket |
| P4 | Alen | Frontend |
| P5 | Abin | DevOps, QA & Documentation |

Branch strategy: all work merges into `develop` via PR; `main` is reserved
for stable milestones only.

---

## Delivery Notes

- The Docker code judge requires Docker Desktop to be running. The first judged submission may take longer while language images are downloaded.
- Google sign-in requires a valid Google OAuth client ID, client secret, and redirect URI in the local environment; never commit those values.
- Password reset and email verification require mail-provider credentials before they can send real email outside local development.

## License

Educational/academic project — no license specified yet.
