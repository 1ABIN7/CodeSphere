# CodeSphere — API Documentation

## Base URL
- Local: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Authentication
All endpoints except `/api/auth/**` require a JWT token in the header:

```
Authorization: Bearer <token>
```

Tokens are issued by `POST /api/auth/login` and refreshed via `POST /api/auth/refresh`.
Roles referenced below (`SUPER_ADMIN`, `ORG_ADMIN`, `EXAMINER`, `INSTRUCTOR`, `CANDIDATE`, plus
legacy `ADMIN`/`MODERATOR` on a couple of older endpoints) are enforced with `@PreAuthorize`.
Endpoints with no role listed just require a valid token (any authenticated user).

> **Known inconsistency:** a few endpoints (`QuestionController`) still check the older
> `ADMIN`/`MODERATOR` roles instead of the current `SUPER_ADMIN`/`ORG_ADMIN`/`EXAMINER` scheme.
> Flagged here so it doesn't get missed — see [Known Issues](../../README.md#known-issues--current-status).

---

## 1. Auth — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/health` | none | Liveness check |
| POST | `/register` | none | Create account. Body: `RegisterRequest { username, email, password }`. Returns `AuthResponse { userId, token, refreshToken, username, role }` |
| POST | `/login` | none | Body: `LoginRequest { username/email, password }`. Returns `AuthResponse` |
| POST | `/refresh` | none | Query param `refreshToken`. Returns new `AuthResponse` |
| POST | `/logout` | token | Denylists current access token |
| POST | `/forgot-password` | none | Body: `ForgotPasswordRequest`. **Not yet implemented** — reset token currently only prints to console, no email is sent |
| POST | `/reset-password` | none | Body: `ResetPasswordRequest`. **Not yet implemented** end-to-end |
| GET | `/verify-email` | none | Query param `token`. **Not yet implemented** end-to-end |

`RegisterRequest` validation: username 3–50 chars (letters/numbers/underscore/dot only), valid email,
password 8+ chars with at least one digit, lowercase, uppercase, and special character.

---

## 2. Coding Problems — `/api/problems`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `` | token | Paginated list — returns `PageResponse<ProblemListResponse>` |
| GET | `/{id}` | token | Single problem — `ProblemResponse` |
| GET | `/{id}/test-cases` | token | `List<TestCaseResponse>` |
| POST | `` | token | Create problem — body `ProblemRequest` |
| PUT | `/{id}` | token | Update problem |
| DELETE | `/{id}` | token | Delete problem |
| PATCH | `/{id}/publish` | token | Toggle publish (requires ≥1 test case) |
| POST | `/{id}/test-cases` | token | Add a test case — body `TestCaseRequest` |
| PUT | `/{problemId}/test-cases/{testCaseId}` | token | Update a test case |
| DELETE | `/{problemId}/test-cases/{testCaseId}` | token | Delete a test case |

## 3. Submissions (synchronous path) — `/api/submissions`

| Method | Path | Description |
|---|---|---|
| POST | `` | Submit code — body `SubmissionRequest`, returns `SubmissionResponse`. Runs synchronously against test cases and includes AI code analysis |
| GET | `/{id}` | `SubmissionResponse` for one submission |
| GET | `/{id}/analysis` | `JudgeResultResponse` — detailed judge output |
| GET | `/problem/{problemId}` | Paginated submissions for a problem — `PageResponse<SubmissionListResponse>` |
| GET | `/me` | Paginated submissions for the current user |

## 4. Submissions (async, queue-based path) — `/api/v1/problems/{id}`

| Method | Path | Description |
|---|---|---|
| POST | `/submit` | Queues submission via RabbitMQ → `SubmissionConsumer` → judge, WebSocket-notified on completion |
| POST | `/run` | Runs against sample cases only (no persistence), for a quick "Run" button in the editor |

> **Note:** this is a separate, not-yet-reconciled implementation from the `/api/submissions` path above.
> Frontend currently talks to whichever one it was wired against — check before assuming both are live in a given build. See [Known Issues](../../README.md#known-issues--current-status).

---

## 5. Assessments — `/api/v1/assessments`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `` | token | All assessments |
| GET | `/{id}` | token | Single assessment |
| GET | `/{id}/assigned-candidates` | token | `List<AssessmentAssignment>` |
| GET | `/{id}/assignment-status` | EXAMINER+ | `List<AssessmentAssignmentStatusDto>` |
| GET | `/available` | token | Assessments available to the current candidate |
| POST | `` | INSTRUCTOR+ | Create assessment |
| PUT | `/{id}` | INSTRUCTOR+ | Update assessment |
| DELETE | `/{id}` | INSTRUCTOR+ | Delete assessment |
| POST | `/{id}/clone` | INSTRUCTOR+ | Deep-clone an assessment |
| PUT | `/{id}/publish` | INSTRUCTOR+ | Publish (runs validation first) |
| PUT | `/{id}/unpublish` | INSTRUCTOR+ | Unpublish |
| POST | `/{id}/assign` | INSTRUCTOR+ | Assign to one or more candidates |
| POST | `/{id}/assign-group` | INSTRUCTOR+ | Assign to a `CandidateGroup` via `groupId` query param |

## 6. Assessment Sections & Questions — `/api/v1/assessments` & `/api/v1/sections`

| Method | Path | Description |
|---|---|---|
| POST | `/assessments/{assessmentId}/sections` | Add a section |
| GET | `/assessments/{assessmentId}/sections` | List sections |
| GET | `/assessments/{assessmentId}/questions` | All questions across the assessment |
| GET | `/assessments/sections/{sectionId}/questions` | Questions in one section |
| PUT | `/assessments/{assessmentId}/sections/reorder` | Reorder sections |
| DELETE | `/assessments/sections/{id}` | Delete section |
| POST | `/sections/{sectionId}/questions` | Attach a question to a section |
| GET | `/sections/{sectionId}/questions` | List questions in a section |
| PUT | `/sections/{sectionId}/questions/reorder` | Reorder questions |
| DELETE | `/sections/questions/{id}` | Remove question from section |

## 7. Assessment Sessions (candidate-facing) — `/api/v1/assessment-sessions`

| Method | Path | Description |
|---|---|---|
| POST | `/{assessmentId}/start` | Start a session — returns `AssessmentSession` |
| POST | `/{assessmentId}/submit` | Submit/finish — returns `AssessmentResultDTO` |
| POST | `/{sessionId}/answers` | Autosave one answer — body `{ questionId, value }` |
| POST | `/{sessionId}/sections/{sectionIndex}/navigate` | Move between sections |
| POST | `/{sessionId}/files` | Upload a file for a file-upload question — query param `questionId` |
| GET | `/results` | `List<AssessmentHistoryItemDto>` — the candidate's full result history |
| GET | `/{assessmentId}/result` | Result for one assessment |

---

## 8. Question Bank — `/api/v1/questions`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `` | EXAMINER+ | Create question |
| GET | `` | token | List questions |
| GET | `/{id}` | token | Single question |
| PUT | `/{id}` | EXAMINER+ | Update question |
| DELETE | `/{id}` | EXAMINER+ | Delete question |
| POST | `/{id}/submit-for-approval` | token | Move to pending-approval state |
| POST | `/{id}/approve` | INSTRUCTOR+ | Approve |
| POST | `/{id}/reject` | INSTRUCTOR+ | Reject |
| GET | `/{id}/versions` | token | Version history |
| POST | `/{id}/restore/{version}` | token | Roll back to a prior version |
| GET | `/export` | token | Bulk export (`byte[]`, e.g. CSV/JSON) |
| POST | `/import` | token | Bulk import — multipart file |
| GET | `/tags/autocomplete` | token | Query param `query` |
| GET | `/tags/trending` | token | Most-used tags |
| POST | `/{id}/rubric` | token | Define a grading rubric |
| GET | `/{id}/rubric` | token | Fetch rubric |
| POST | `/{questionId}/approve` | ADMIN/MODERATOR *(legacy roles — see note above)* | Older approval endpoint, overlaps with #approve above |
| POST | `/{questionId}/reject` | ADMIN/MODERATOR *(legacy roles)* | Older rejection endpoint |

## 9. Categories — `/api/v1/categories`

| Method | Path | Description |
|---|---|---|
| POST | `` | Create category |
| GET | `` | Category tree |
| DELETE | `/{id}` | Delete category |

## 10. Reading Comprehension — `/api/v1/comprehension`

| Method | Path | Description |
|---|---|---|
| GET | `/session/{sessionId}/passage/{passageId}` | `ComprehensionViewDto` — passage + sub-questions |
| POST | `/session/{sessionId}/sub-questions/{subQuestionId}/submit` | Submit an answer to one sub-question |

## 11. Manual Evaluation — `/api/v1/evaluations`

*(all endpoints require EXAMINER+)*

| Method | Path | Description |
|---|---|---|
| GET | `/pending` | `List<EvaluationQueueItem>` awaiting manual grading |
| PUT | `/{answerId}` | Submit a grade — body `EvaluationRequest` |
| GET | `/{answerId}/file` | Download the file attached to a file-upload answer |

---

## 12. Users & Profile — `/api/v1/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/candidates` | EXAMINER+ | `List<CandidateOptionDto>` — for assignment dropdowns |
| GET | `` | EXAMINER+ | `List<AdminUserDto>` — all users |
| GET | `/{username}/profile` | token | Public profile |
| PUT | `/me/profile` | token | Update own profile |
| GET | `/me/dashboard` | token | `UserDashboardDto` — stats for the logged-in user |
| GET | `/me/submissions` | token | Paginated `SubmissionHistoryDTO` |
| GET | `/me/exams` | token | Paginated `ExamHistoryDTO` |
| GET | `/me/certifications` | token | `List<CertificationDTO>` — **certification issuance itself isn't implemented yet**, so this will be empty |
| GET | `/me/recommendations` | token | `List<AssessmentRecommendationDTO>` |

## 13. Candidate Groups — `/api/v1/candidate-groups`

*(all endpoints require EXAMINER+)*

| Method | Path | Description |
|---|---|---|
| GET | `` | List groups |
| POST | `` | Create — body `{ name, memberUserIds }` |
| DELETE | `/{id}` | Delete |

---

## 14. Interview Prep — `/api/interview`

| Method | Path | Description |
|---|---|---|
| GET | `/categories` | `List<InterviewCategoryResponse>` |
| POST | `/sessions` | Start a practice session |
| GET | `/sessions/{id}/next` | Next question in the session |
| POST | `/sessions/{id}/answers` | Submit an answer — returns `AttemptFeedbackResponse` |
| POST | `/sessions/{id}/complete` | End session — returns `SessionResultResponse` |
| GET | `/sessions/{id}/result` | Fetch a completed session's result |
| GET | `/performance` | `PerformanceSummaryResponse` — aggregate stats across sessions |

---

## 15. Files & Storage

### `/api/files` (entity-attached files — DB-tracked metadata)

| Method | Path | Description |
|---|---|---|
| POST | `/upload` | Multipart upload — returns `FileUploadResponse` |
| GET | `/{id}` | Download (`Resource`) |
| GET | `/{id}/metadata` | Metadata only |
| DELETE | `/{id}` | Delete |
| GET | `/entity/{entityType}/{entityId}` | All files attached to an entity (e.g. a submission or question) |

### `/api/v1/storage`

| Method | Path | Description |
|---|---|---|
| POST | `/upload` | Lower-level upload direct to the configured backend (local disk or MinIO) |

---

## 16. Proctoring — `/api/proctoring`

| Method | Path | Description |
|---|---|---|
| POST | `/sessions/{sessionId}/events` | Log a proctoring event (tab-switch, focus-loss, etc.) |
| POST | `/sessions/{sessionId}/snapshot` | Upload a webcam snapshot |
| GET | `/sessions/{sessionId}/events` | All events for a session |
| GET | `/assessments/{assessmentId}/config` | Get proctoring config for an assessment |
| PUT | `/assessments/{assessmentId}/config` | Update proctoring config |
| PUT | `/sessions/{sessionId}/flag` | Manually flag a session for review |

---

## 17. Admin — `/api/v1/admin`

*(all endpoints require SUPER_ADMIN / ORG_ADMIN, most also allow EXAMINER)*

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/dashboard` | EXAMINER+ | `AdminDashboardResponse` |
| GET | `/reports/assessments` | EXAMINER+ | `AssessmentReportResponse` |
| GET | `/reports/questions` | EXAMINER+ | `QuestionAnalyticsResponse` |
| GET | `/reports/candidates` | EXAMINER+ | `CandidateAnalyticsResponse` |
| PUT | `/submissions/{submissionId}/score` | EXAMINER+ | Manually override a submission's score |
| PUT | `/users/{targetUserId}/role` | SUPER_ADMIN/ORG_ADMIN only | Change a user's role |
| GET | `/audit-logs` | ADMIN *(legacy role)* | Paginated, filterable `Page<AuditLog>` |

---

## Response conventions

- Paginated endpoints return `PageResponse<T>` (custom wrapper) or Spring's `Page<T>`, depending on
  which controller — not yet standardized on one, see [Known Issues](../../README.md#known-issues--current-status).
- Validation errors and other exceptions are handled centrally by `GlobalExceptionHandler` and
  returned as `ApiErrorResponse`.
- All timestamps are UTC ISO-8601.

## Not yet implemented (no working endpoints for these)

- Certification issuance/verification (the DTO and the `/me/certifications` read endpoint exist,
  but nothing ever populates it)
- Email notifications (register/forgot-password print tokens to console instead of sending mail)
- Plagiarism detection