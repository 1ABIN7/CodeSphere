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
