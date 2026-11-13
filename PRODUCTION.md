# Production roadmap — Projects Manager (PSM)

Last updated: 2026-11-13

Status: ✅ done · 🟡 partial · ⬜ next

---

## 1. What is already in place

- JWT authentication, BCrypt password hashing, ADMIN / COLLABORATEUR roles
- Spring Security, request validation, global error handling
- Clients and projects CRUD with a consistent `entity → repository → service → controller → dto` layout
- Angular 22 standalone UI with guards, interceptors and a shared error notification
- SQLite in Docker, persisted in the `/var/lib/psm` volume
- Docker image that serves the API and the SPA from the same origin
- Backend tests + a frontend production build

## 2. Just shipped — secure file & image uploads ✅

- `Attachment` entity, repository, DTO, service and controller
- Local storage abstraction (`FileStorageService`) with:
  - configurable root (`PSM_STORAGE_ROOT`)
  - 10 MB per-file limit (configurable)
  - content-type + extension whitelist (images, PDF, Office, CSV, ZIP)
  - generated UUID storage keys and path-traversal protection
  - original file names sanitized
- Authenticated upload / list / download / delete endpoints
- `Content-Disposition` + `X-Content-Type-Options: nosniff` on downloads
- Only the uploader or an ADMIN can delete a file
- `/files` page: drag & drop, upload progress, project/category filters, image thumbnails, full-screen preview, download, delete
- Unit tests for the storage rules
- Docker: files live in `/var/lib/psm/uploads`, inside the existing volume

Known limitation: any authenticated user can read any file by id until project
membership exists. This is tracked below (P0 — authorization).

## 3. Production blockers (P0)

These are not "nice to have". A real company cannot run the app without them.

### Security

1. **Protect and rotate runtime credentials.** `JWT_SECRET` and
   `PSM_ADMIN_PASSWORD` are supplied at runtime and are not included in the
   Docker image. Use Render-generated environment values or an ignored local
   environment file; never commit them.
2. **Keep the JWT secret required.** The app intentionally fails fast if
   `JWT_SECRET` is missing; configure it in the deployment environment.
3. **HTTPS everywhere** behind Caddy/Nginx/Traefik, with HSTS and secure headers
   (CSP, `X-Frame-Options`, `Referrer-Policy`).
4. **Rate limiting + lockout** on login, register and password reset. Add a small
   in-memory or Redis-backed filter.
5. **Short-lived access tokens + refresh tokens**, and token revocation on logout /
   password change. Today a token stays valid for 24 h.
6. **Email verification and password reset** using the existing Resend integration.
7. **2FA (TOTP) for ADMIN accounts.**
8. **Permission model**: replace hard-coded role checks with a permission matrix
   (`PROJECT_READ`, `INVOICE_WRITE`, …) and enforce it with `@PreAuthorize`.
9. **Project membership**: `ProjectMember(project, user, projectRole)` and scope
   every project, task, file and invoice query to members. This closes the
   attachment IDOR.
10. **Audit log, soft delete and GDPR export/delete** for users and client data.

### Platform

11. **Automated backups** of SQLite and the uploads directory, stored off the VPS,
    with a monthly restore drill. SQLite: `VACUUM INTO` or a stopped-file copy.
12. **Schema migrations** (Flyway/Liquibase) instead of `ddl-auto=update`. SQLite
    needs the matching Flyway module; test the baseline on a copy first.
13. **Health / readiness endpoints** (Spring Actuator: `/actuator/health`,
    `/actuator/info`, `/actuator/metrics`) and a public liveness probe.
14. **Error tracking + structured logs** (Sentry or GlitchTip, JSON logs with
    request ids) and uptime monitoring with alerts.
15. **CI/CD**: build + test + lint on every push, dependency and container scanning
    (Dependabot/Trivy), and a tagged release process.
16. **File pipeline hardening**: antivirus scan (ClamAV), per-user/project quotas,
    thumbnail generation, S3/R2 storage with signed URLs when you outgrow the VPS
    disk, and a retention policy.
17. **Transactional email**: verified sending domain, retry/outbox, templates.
18. **Pagination everywhere** (`Pageable` + `PageResponse<T>`) before data grows.

## 4. Core product features a company will expect (P1)

| Area | What to add | Why it matters |
|---|---|---|
| Project depth | status, priority, start/end dates, progress %, manager, code | Dashboards, planning, client reporting |
| Team | `ProjectMember` with allocation % and daily rate | Who works on what, real cost |
| Work | `Task` (+ subtasks), Kanban, `Milestone`, tags | The actual delivery workflow |
| Time | `TimeEntry` (billable flag, rate), timesheet screen | Actuals, payroll, invoicing |
| Money | `Expense`, `Invoice` + lines + payments, `Quote` | Revenue and project profitability |
| Collaboration | comments, activity feed, notifications (in-app + email) | Retention, fewer status meetings |
| Clients | contacts, industry, status, logo, client detail page | Real CRM basics |
| Insight | reports (budget vs actual, utilization, overdue), calendar | Decisions, not just CRUD |
| Search | global search across projects/clients/files | Usability at scale |
| Settings | workspace name, currency, tax rate, branding | Multi-company readiness |
| Export | CSV/PDF for invoices, projects, time | Accounting and clients |

## 5. Scale & polish (P2)

- Migrate from SQLite to PostgreSQL when you have more than a handful of concurrent
  writers; SQLite allows one writer at a time.
- Multi-tenant workspaces if several companies will use the same instance.
- i18n (French/English), accessibility audit, mobile layout, dark mode.
- Onboarding, empty states, demo data, in-app help.
- E2E tests (Playwright) for login, project CRUD, upload and invoicing.
- Lint + Prettier in CI, frontend unit tests.

## 6. How to add a new entity without breaking the structure

Backend:

1. `entity/X.java` (+ enums under `entity/enums/`)
2. `repository/XRepository.java`
3. `dto/XRequest.java` / `dto/XResponse.java` (records)
4. `service/XService.java` (`@Transactional`)
5. `controller/XController.java` (`/api/x`)
6. authorization rule (`@PreAuthorize` once the permission model lands)
7. migration

Frontend:

8. `models.ts`
9. `services/x.service.ts`
10. `components/x/x.component.ts`
11. `app.routes.ts`
12. navbar link
13. `pageTitles` entry in `app.ts`

## 7. Environment variables

Set secrets in the runtime environment. The Docker image does not bake in
credentials.

| Variable | Purpose | Default behavior |
|---|---|---|
| `PSM_ADMIN_EMAIL` | Bootstrap admin account | `admin@psm.com` |
| `PSM_ADMIN_PASSWORD` | Bootstrap admin password | Required for a usable deployment |
| `JWT_SECRET` | Token signing key | Required; supply a strong secret |
| `RESEND_API_KEY` | Transactional email | Optional |
| `RESEND_FROM_EMAIL` | Sender | Optional |
| `PSM_SQLITE_PATH` | SQLite file | `/var/lib/psm/psm.db` in Docker |
| `PSM_STORAGE_ROOT` | Upload directory | `/var/lib/psm/uploads` in Docker |
| `PSM_STORAGE_MAX_FILE_SIZE` | Max upload bytes | `10485760` |
| `PORT` | HTTP port | `2026` locally; Render sets this automatically |

## 8. Recommended build order

| Phase | Scope | Rough effort |
|---|---|---|
| 0 | Secrets, HTTPS, backups, health checks, CI, migrations | 1–2 weeks |
| 1 | Auth hardening (refresh, reset, verification, rate limit) | 1–2 weeks |
| 2 | Permissions + project membership (closes file IDOR) | 1 week |
| 3 | Project depth: status, members, milestones, tasks/Kanban | 2–3 weeks |
| 4 | Time + money: time entries, expenses, invoices, profitability | 2–3 weeks |
| 5 | Collaboration + notifications + comments + activity | 2 weeks |
| 6 | Reports, calendar, search, exports, settings | 2 weeks |
| 7 | Hardening: antivirus, quotas, S3, E2E tests, monitoring | ongoing |

Phase 0 and 2 are the difference between a demo and something a company can trust.
