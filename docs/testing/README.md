# CodeSphere — Testing

## Automated test coverage and evidence

The backend uses JUnit 5 and Mockito for fast service-level tests, plus a Spring context-load test.

| Test file | Type | Covers |
|---|---|---|
| `BackendApplicationTests` | Context load | Verifies the Spring context boots cleanly |
| `AuthServiceTest` | Unit (Mockito) | Register/login happy paths + duplicate-user rejection |
| `ProblemServiceTest` | Unit (Mockito) | Problem CRUD, publish/unpublish guard, generated question-bank linkage |
| `AssessmentServiceImplTest` | Unit (Mockito) | Assessment CRUD, publish validation, deep clone, candidate assignment |
| `SubmissionServiceTest` | Unit (Mockito) | Submission validation guards, judging flow, owner-only access check |
| `AssessmentSessionServiceImplTest` | Unit (Mockito) | Server-enforced section timing and navigation |
| `SqlAssessmentRunnerServiceImplTest` | Unit | Read-only SQL validation and isolated execution behavior |

### Recorded evidence

| Check | Command | Result |
|---|---|---|
| Full backend suite | `cd backend && ./mvnw test` | **39 tests passed, 0 failures, 0 errors** (July 30, 2026) |
| Frontend timer suite | `cd frontend && npm test` | **3 tests passed, 0 failures** (July 30, 2026) |
| Backend compilation | `cd backend && ./mvnw -DskipTests compile` | Build successful (July 30, 2026) |
| Frontend production build | `cd frontend && npm run build` | Build successful (July 30, 2026) |

**Not covered at all yet:**
- Integration tests (no TestContainers setup — nothing exercises a real Postgres/Redis/RabbitMQ/MinIO)
- `ProctoringService`, `FileService`, `InterviewService`
- The async, queue-based submission path (`ProblemSubmissionController` → RabbitMQ → `SubmissionConsumer` → `DockerExecutionService`)
- Full browser-driven candidate/admin flows
- Broad frontend component/browser-flow tests beyond assessment timing
- Load/performance testing

## Test setup

Tests run against an in-memory H2 database configured to mimic Postgres
(`backend/src/test/resources/application.yml`), so `mvn test` works without
Docker or a real database running. Key details:
- Flyway is disabled for tests — Hibernate auto-creates the schema (`ddl-auto: update`) instead of applying real migrations. This means a broken Flyway migration won't be caught by `mvn test`; only manual `docker compose up` will surface that.
- The suite does not require a running broker to pass. The context-load test may emit RabbitMQ connection warnings when no local broker is present; this is recorded as an environment warning, not a test failure.
- JWT secret is a dummy value baked into the test config — never reuse this for anything real.

## Running tests

```bash
cd backend
mvn test                                  # full suite
mvn test -Dtest=ProblemServiceTest        # single class
mvn test -Dtest=SubmissionServiceTest#submitCode_Success_AcceptedUpdatesStatsAndSkillScore  # single test
```

CI runs `mvn test` automatically on every push/PR via `.github/workflows/ci.yml`.

## Conventions

- JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`) — no Spring context needed for pure service-layer unit tests, keeps them fast.
- One test class per service, named `<ServiceName>Test`.
- Test method names follow `methodUnderTest_ExpectedBehavior_WhenCondition` (e.g. `togglePublish_ThrowsException_WhenNoTestCases`).
- Prefer `ArgumentCaptor` over loose `any()` matchers when asserting on *what* got saved, not just *that* something got saved.

## Priorities for remaining test work

Roughly in order of value vs. effort, given the demo deadline:
1. `ProctoringService` — smallest surface area, quick win
2. `FileService` — storage abstraction, straightforward to mock
3. Integration test for the synchronous submission path (`/api/submissions`) using TestContainers + a real Postgres, since this is the flow most likely to be demoed live
4. `InterviewService`
