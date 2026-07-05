# CodeSphere Phase 1 MVP — Week 1 Progress Report
**Team Member:** Anup  
**Role:** Backend Developer — Database & Config  
**Dates:** June 30 – July 6, 2026  

---

## 🏆 Accomplishments & Deliverables

This week, I successfully initialized the project's dependency structure, database schema, profile configurations, and API security gatekeeping filters. 

Below is a detailed breakdown of the deliverables:

### 1. Project Foundation & Dependency Management
*   **Build File (`pom.xml`)**: Configured the Maven build manager for Java 21, Spring Boot 3.3.1, Spring Security, Validation, PostgreSQL Driver, Lombok, and Flyway.
*   **Application Boot (`CodeSphereApplication.java`)**: Created the main class using `@SpringBootApplication` annotations to enable component scanning and boot up the Tomcat web server.

### 2. Flyway Database Migrations (V1 & V2)
I authored the database schemas to support stateless users and the judge execution engine:
*   **V1 Migration (`V1__init_schema.sql`)**:
    *   Constructed the relational schemas for `organizations`, `users`, `roles`, and the `user_roles` many-to-many lookup table.
    *   Seeded three roles (`ROLE_USER`, `ROLE_ADMIN`, and `ROLE_ORGANIZATION_ADMIN`) to unblock backend authentication development.
*   **V2 Migration (`V2__problems_submissions.sql`)**:
    *   Created the table `problems` (containing descriptions, difficulty labels, execution run limits like `time_limit` and `memory_limit`).
    *   Created the table `test_cases` (mapping test input/output datasets to coding problems).
    *   Created the table `submissions` (logging language, code source, statuses like `ACCEPTED` or `WRONG_ANSWER`, execution duration, memory consumption, and error stacktraces).
    *   Created query optimization indexes on key relational attributes (`submissions(user_id)` and `submissions(problem_id)`).

### 3. Application Configurations (YAML Profiles)
*   **Default Profile (`application.yml`)**: Setup server running on port `8080`, disabled Open Session In View (OSIV) for performance, and established default JWT access token lifespans (15 mins) and refresh token rotations (7 days).
*   **Development Profile (`application-dev.yml`)**: Wired JDBC connection strings, local Redis connections, RabbitMQ connections, and MinIO storage configs.
*   **Custom Setup**: Adjusted the connection password default to `Atulanup20` to seamlessly bind PostgreSQL services.

### 4. Spring Security 6.x Filter Chain (`SecurityConfig.java`)
*   **CORS Filter**: Created policy rules allowing connections from the React development server (`http://localhost:5173`).
*   **CSRF & Sessions**: Disabled CSRF and switched sessions to stateless mode (`SessionCreationPolicy.STATELESS`) to support bearer JWT token verification.
*   **Authorization Rules**:
    *   *Public Routes:* `/auth/**` (Registration, login, reset password), Swagger documentation, and GET requests on `/problems/**` (viewing problem lists).
    *   *Protected Routes:* All other HTTP endpoints require standard JWT authentication.

---

## 🔍 Verification & Visual Inspection (pgAdmin)

The database schema migrations were successfully run and visually verified using pgAdmin:
*   **Connection**: Configured connection to local server host `localhost:5432` and database `codesphere`.
*   **Visual Validation**: Confirmed that all **8 tables** are present under the public schema:
    1.  `organizations`
    2.  `users`
    3.  `roles`
    4.  `user_roles`
    5.  `problems`
    6.  `test_cases`
    7.  `submissions`
    8.  `flyway_schema_history` (created by Flyway for schema state tracking)

All tasks assigned for Week 1 have been completed, verified, and are ready for the Week 2 codebase integrations.
