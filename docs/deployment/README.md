# CodeSphere — Deployment Guide

## Prerequisites

Make sure you have the following installed before running the project:

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 21 | Backend runtime |
| Maven | 3.9+ | Backend build tool |
| Node.js | 18+ | Frontend runtime |
| Docker | 24+ | Containerization |
| Docker Compose | 2.0+ | Multi-container orchestration |
| PostgreSQL | 16 | Database (if running locally without Docker) |

---

## Quick Start (Docker Compose — Recommended)

This runs the entire stack with one command.

### Step 1 — Clone the repository
```bash
git clone https://github.com/1ABIN7/CodeSphere.git
cd CodeSphere
```

### Step 2 — Set up environment variables
```bash
cp .env.example .env
```
Open `.env` and fill in your local values.

### Step 3 — Start all services
```bash
docker-compose up --build
```

### Step 4 — Verify everything is running
| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:5050 |
| RabbitMQ UI | http://localhost:15672 |
| MinIO Console | http://localhost:9001 |

---

## Manual Setup (Without Docker)

### Backend

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| DB_HOST | PostgreSQL host | localhost |
| DB_PORT | PostgreSQL port | 5432 |
| DB_NAME | Database name | conflux_db |
| DB_USERNAME | Database username | postgres |
| DB_PASSWORD | Database password | yourpassword |
| JWT_SECRET | JWT signing secret | your_jwt_secret_key |
| JWT_EXPIRATION | JWT expiry in ms | 86400000 |
| SERVER_PORT | Backend server port | 8080 |
| FRONTEND_URL | Frontend origin for CORS | http://localhost:5173 |
| GEMINI_API_KEY | Server-only Gemini key for Coding Help and Admin AI Insights | obtain from Google AI Studio |
| GEMINI_MODEL | Optional Gemini model override | gemini-3.6-flash |

Never commit `.env`. In production, use your deployment platform’s secret manager instead of a file. `GEMINI_API_KEY` must remain on the backend and must not be added to Vite (`VITE_*`) variables.

---

## Health Check

Once the backend is running, verify it's healthy:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## Branch Strategy (For Developers)

- Always clone from `develop` branch
- Create feature branches: `git checkout -b feature/your-feature-name`
- Never push directly to `main` or `develop`
- Raise a PR and wait for P5 review before merging

---

*Document maintained by P5 — Abin Joseph*
*Last updated: July 2026*
