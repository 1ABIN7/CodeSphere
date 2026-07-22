# CodeSphere Frontend

React 19 + Vite + plain JavaScript assessment platform.

## Quick Start

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` and proxies `/api` to the backend at `http://127.0.0.1:8080`.

## Environment Variables

Copy `.env.example` to `.env` and fill in:

| Variable | Required | Description |
|---|---|---|
| `VITE_API_URL` | No | Backend URL. Dev proxy handles this; only needed for production builds. |

## Implemented So Far

### Working Features (pre-existing)
- **LoginPage / RegisterPage** — email/password auth with JWT, Google OAuth button, forgot/reset password, email verification
- **AuthContext** — `{ user, token, login, logout, isAdmin, isLoggedIn }`
- **Navbar** — role-aware (candidate / evaluator / admin / instructor)
- **HomePage** — full marketing landing page (hero, 6 assessment types, how-it-works, trust strip, practice hub, footer)
- **ProblemsPage / ProblemDetailPage** — Practice Hub with Monaco editor, test case runner
- **DashboardPage** — candidate dashboard with stats, skill breakdown, quick actions
- **SubmissionsPage** — submission history table

### Steps 0–7 Build-Out
| Step | Status |
|---|---|
| 0. Orientation | Complete — all 8 controllers, 12 migrations, enums verified |
| 1. Auth + Google OAuth | Complete — backend OAuth2 (V11), forgot/reset/verify pages, polish |
| 2. Landing page | Complete — professional SaaS marketing page |
| 3. Candidate assessment flow | Complete — AssessmentDashboard, AssessmentPage (6 renderers), AssessmentResultPage |
| 4. Admin panel | Complete — 9 admin pages + DataTable + QuestionEditorModal |
| 5. Proctoring UI | Complete — ProctoringBar, BehaviorTracker, WebcamMonitor (TODO: backend) |
| 6. Profile / Certs / Notifs | Complete — ProfilePage, CertificationPage, NotificationsPage |
| 7. Polish | Complete — lint clean, build passes, skeletons, error states, responsive |

### Backend (added for Step 1)
- `pom.xml` — added `spring-boot-starter-oauth2-client`
- `V11__add_oauth_provider.sql` — `oauth_provider` column on users
- `OAuth2LoginSuccessHandler.java` — find-or-create user, issue JWT
- `SecurityConfig.java` — `.oauth2Login()` block
- `application.yml` — Google OAuth2 client config
- `.env.example` — `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` placeholders

---

## Route-to-Endpoint Map

### Public Routes (PublicLayout with Navbar)
| Route | Component | Backend Endpoint(s) |
|---|---|---|
| `/` | HomePage | None (static) |
| `/login` | LoginPage | `POST /api/auth/login` |
| `/register` | RegisterPage | `POST /api/auth/register` |
| `/forgot-password` | ForgotPasswordPage | `POST /api/auth/forgot-password` (TODO) |
| `/reset-password` | ResetPasswordPage | `POST /api/auth/reset-password` (TODO) |
| `/verify-email` | VerifyEmailPage | `GET /api/auth/verify-email?token=` (TODO) |
| `/auth/callback` | AuthCallbackPage | OAuth2 redirect handler |
| `/problems` | ProblemsPage | `GET /api/problems` |
| `/problems/:id` | ProblemDetailPage | `GET /api/problems/:id`, `POST /api/submissions`, `GET /api/submissions/:id` |
| `*` | 404 Page | None |

### Candidate Routes (CandidateLayout with Sidebar)
| Route | Component | Backend Endpoint(s) |
|---|---|---|
| `/dashboard` | DashboardPage | `GET /api/submissions/me` |
| `/submissions` | SubmissionsPage | `GET /api/submissions/me` |
| `/assessments` | AssessmentDashboard | `GET /api/assessments/mine` (TODO) |
| `/assessments/:id/session` | AssessmentPage | `GET /api/v1/assessments/:id`, `POST /api/assessments/:id/session/start`, `GET /api/assessments/:id/session`, `PUT /api/assessments/:id/session/answers`, `POST /api/assessments/:id/session/submit` |
| `/assessments/:id/result` | AssessmentResultPage | `GET /api/assessments/:id/result` (TODO) |
| `/profile` | ProfilePage | `GET /api/submissions/me` |
| `/certifications` | CertificationPage | TODO (CertificationController) |

### Admin Routes (AdminLayout with Sidebar)
| Route | Component | Backend Endpoint(s) |
|---|---|---|
| `/admin/dashboard` | AdminDashboardPage | `GET /api/v1/assessments` |
| `/admin/assessments` | AssessmentListPage | `GET /api/v1/assessments`, `DELETE /api/v1/assessments/:id`, `POST /api/v1/assessments/:id/clone`, `PUT /api/v1/assessments/:id/publish`, `PUT /api/v1/assessments/:id/unpublish` |
| `/admin/assessments/new` | AssessmentWizardPage | `POST /api/v1/assessments`, `POST /api/v1/assessments/:id/sections`, `POST /api/v1/sections/:sectionId/questions`, `POST /api/v1/assessments/:id/assign` |
| `/admin/assessments/:id/edit` | AssessmentWizardPage | `PUT /api/v1/assessments/:id`, section/question CRUD |
| `/admin/questions` | QuestionBankPage | TODO (QuestionBankController) |
| `/admin/questions/import` | QuestionImportPage | TODO (QuestionImportController) |
| `/admin/questions/approval` | QuestionApprovalPage | TODO (QuestionBankController) |
| `/admin/evaluations` | EvaluatorDashboard | TODO (no controller) |
| `/admin/reports` | ReportsDashboard | TODO (no controller) |
| `/admin/audit-logs` | AuditLogPage | TODO (no controller) |
| `/admin/notifications` | NotificationsPage | TODO (no controller) |

---

## Backend Gaps (missing controllers — no endpoints exist)

| Feature | Missing Controller | DB Tables Present |
|---|---|---|
| Assessment session (start/submit/resume) | AssessmentSessionController | `assessment_sessions`, `session_answers` |
| Question bank CRUD | QuestionBankController | `question_bank`, `question_versions`, `rubrics` |
| Question import/export | QuestionImportController, QuestionExportController | `question_bank` |
| Question approval workflow | QuestionBankController | `question_bank.is_approved` |
| Evaluation / manual grading | (none) | `session_answers`, `evaluation_reviews` |
| Reports / analytics | (none) | queryable from `assessment_sessions`, `session_answers` |
| Audit logs | AuditLogController | `audit_logs` |
| Notifications | NotificationController | (no table) |
| Certifications | CertificationController | `certifications` |
| Proctoring | ProctoringController | `proctoring_events` |
| User profile / admin | UserController, AdminController | `users`, `organizations` |
| Assessment session endpoints | AssessmentSessionController | `assessment_sessions`, `session_answers` |
