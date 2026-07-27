# CodeSphere

A coding-assessment and interview-preparation platform — think a lightweight
HackerRank/CoderPad hybrid. Candidates solve coding problems and take
multi-format assessments (MCQ, coding, written, reading comprehension, file
upload); examiners create problems, question banks, and assessments;
proctoring and evaluation tooling sit on top.

Built as a BTech final-year team project.

> **Status:** actively in development ahead of a scheduled demo. See
> [Known Issues & Current Status](#known-issues--current-status) below for
> an honest picture of what's live vs. in progress — this project is not
> feature-complete yet.

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

**Planned, not yet live** (see [Known Issues](#known-issues--current-status)):
- Question bank management (create/approve/version questions)
- Full assessment-taking experience for candidates (session timers, autosave, resume)
- MCQ/written/reading-comprehension/file-upload assessment engines
- Manual evaluation workflow for reviewers
- Reporting & analytics dashboards
- Email notifications, certifications, plagiarism detection

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

> ⚠️ There is a second, more complete frontend implementation sitting at the
> repo root (`/src`, `/public`, `/vite.config.ts`) alongside `/frontend`.
> `docker-compose.yml` and `Dockerfile.frontend` currently build `/frontend`.
> The two haven't been reconciled yet — see
> [Known Issues](#known-issues--current-status).

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
├── src/, public/              # A second, separate frontend build (see note above)
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

- [API Reference](docs/api/README.md)
- [Architecture](docs/architecture/README.md)
- [Database Schema](docs/database/README.md)
- [Deployment Guide](docs/deployment/README.md)
- [Testing](docs/testing/README.md)

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

## Known Issues & Current Status

This section exists so anyone picking up the repo — including future us —
doesn't waste time assuming more is finished than actually is:

- **Two frontend implementations exist** (`/frontend` vs. root `/src`). The
  root version is more feature-complete but isn't the one currently wired
  into Docker Compose.
- **Two parallel submission/judging implementations exist** (`/api/submissions`,
  synchronous, vs. `/api/v1/problems/{id}/submit`, async/queue-based). Not
  yet reconciled into one path.
- **A significant amount of backend code — the full Question Bank suite,
  assessment session lifecycle, MCQ/written/reading-comprehension/file-upload
  engines, and some security-hardening services — currently sits outside the
  Maven build path** (`backend/controller/`, `backend/service/`, etc. at the
  repo root, instead of under `backend/src/main/java/...`) and is **not
  compiled into the running application**. This needs to be moved before
  those features can be demoed or tested.
- Forgot-password, reset-password, and email verification are not
  implemented yet, despite supporting DTOs/entity fields already existing.
- Manual evaluation workflow, reporting/analytics, notifications, and
  certifications have not been started.

See [docs/architecture/README.md](docs/architecture/README.md) for more detail.

## License

Educational/academic project — no license specified yet.