# CodeSphere

Online Assessment Platform — Caytm Technologies Internship Project

## Tech Stack
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL
- Frontend: React + Vite
- DevOps: Docker, Nginx, GitHub Actions

## Getting Started

### Prerequisites
- Java 21
- Maven
- PostgreSQL
- Node.js

### Setup

1. Clone the repo
   git clone https://github.com/1ABIN7/CodeSphere.git

2. Create your environment file
   cp .env.example .env
   Fill in your local values in .env

3. Run the backend
   cd backend
   mvn spring-boot:run

4. Run the frontend
   cd frontend
   npm install
   npm run dev

## Branch Strategy
- main → stable production code only
- develop → active development branch
- Always push to develop, never to main
- Raise a PR from develop into main for releases

## Implemented So Far

### Backend
- **Database Schema (Flyway)**: `organizations`, `users`, `roles`, `user_roles`, `problems`, `test_cases`, and `submissions` tables, with seeded roles (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_ORGANIZATION_ADMIN`)
- **Authentication API** (`/api/auth`):
  - `POST /register` — user registration
  - `POST /login` — login with JWT issuance
  - `POST /logout`
  - `POST /forgot-password` and `POST /reset-password`
  - `GET /verify-email` — email verification
- **Security**: Spring Security 6.x filter chain, stateless sessions, JWT access + refresh token rotation (`JwtTokenProvider`, `RefreshTokenService`)
- **Audit Logging**: `AuditLog` entity and `AuditLogService` for tracking user/admin actions
- **Role-Based Access**: role checks wired into the security layer for user/admin/org-admin permissions
- **Data Seeding**: `DataSeeder` for populating demo data in the dev profile
- **API Docs**: SpringDoc OpenAPI / Swagger UI configured
- **Docs**: architecture overview, database schema, and deployment guide added under `docs/`

### Frontend
- Vite + React 18 + TypeScript scaffold
- Redux store for global state management
- Centralized Axios instance for API calls
- In progress: login/register pages, protected routing, and a role-aware `AppLayout` sidebar

### DevOps
- Dockerfiles for backend and frontend, Nginx config
- GitHub Actions CI pipeline (backend build + tests, using H2 in-memory DB for the test profile)