# CodeSphere — Testing

## Current state (honest snapshot)

Test coverage is early-stage. As of this writing:

| Test file | Type | Covers |
|---|---|---|
| `BackendApplicationTests` | Context load | Verifies the Spring context boots cleanly |
| `AuthServiceTest` | Unit (Mockito) | Register/login happy paths + duplicate-user rejection |
| `ProblemServiceTest`* | Unit (Mockito) | Problem CRUD, publish/unpublish guard (requires ≥1 test case) |
| `AssessmentServiceImplTest`* | Unit (Mockito) | Assessment CRUD, publish validation, deep clone, candidate assignment |
| `SubmissionServiceTest`* | Unit (Mockito) | Submission validation guards, judging flow, owner-only access check |

\* Drafted and verified against actual source, not yet merged into `develop` as of this doc's last update — add them to this table's "done" status once pushed.

**Not covered at all yet:**
- Integration tests (no TestContainers setup — nothing exercises a real Postgres/Redis/RabbitMQ/MinIO)
- `ProctoringService`, `FileService`, `InterviewService`
- The async, queue-based submission path (`ProblemSubmissionController` → RabbitMQ → `SubmissionConsumer` → `DockerExecutionService`)
- Anything in the Question Bank / session-lifecycle code, since that code isn't even compiled into the build yet (see [Architecture](../architecture/README.md))
- Frontend tests (no test runner configured in either frontend tree)
- Load/performance testing

## Test setup

Tests run against an in-memory H2 database configured to mimic Postgres
(`backend/src/test/resources/application.yml`), so `mvn test` works without
Docker or a real database running. Key details:
- Flyway is disabled for tests — Hibernate auto-creates the schema (`ddl-auto: update`) instead of applying real migrations. This means a broken Flyway migration won't be caught by `mvn test`; only manual `docker compose up` will surface that.
- RabbitMQ listener auto-startup is disabled, so tests don't need a running broker.
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

## Known limitation worth flagging to the team

A meaningful chunk of backend functionality (Question Bank, assessment
session lifecycle, the four remaining assessment-type engines) currently
lives outside the Maven build path and isn't compiled. **Writing tests
against that code now would be testing something that doesn't run.**
Once it's moved into `backend/src/main/java/...`, this doc and the test
suite both need a follow-up pass.

## Priorities for remaining test work

Roughly in order of value vs. effort, given the demo deadline:
1. `ProctoringService` — smallest surface area, quick win
2. `FileService` — storage abstraction, straightforward to mock
3. Integration test for the synchronous submission path (`/api/submissions`) using TestContainers + a real Postgres, since this is the flow most likely to be demoed live
4. `InterviewService`
