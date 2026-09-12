# ProjectSphere SRS coverage — alpha 0.1

The entire SRS is **not complete**. This delivery provides a working core, real-account login, backend source and tests. The Java backend has not been deployed to an external host.

| SRS areas | Implemented | Remaining |
|---|---|---|
| 1–9 roles/hierarchy | Custom roles, CRUD, one role/user, reporting tree, project managers, organization scope | Multiple roles, CTO portfolio scope, final policy review |
| 10 authentication | Login, BCrypt, sessions, logout, CSRF, lockout, password change and email recovery | JWT/refresh, optional SSO/MFA |
| 11 login/logout tracking | Audit events, session invalidation | Dedicated session history with IP/device/browser/duration |
| 12–13 attendance | Explicit check-in/out, breaks, net hours | Login-derived attendance, holidays/leave/weekends, half-day/absence policies, corrections |
| 14, 46–50 dashboards | Permission-aware overview, project/task/sprint summaries | Dedicated CEO/CTO/PM/lead dashboards and full metrics |
| 15–16 projects | CRUD/archive, dates/client/technology/budget, teams/managers, derived membership | Project health, budget ledger, learning requirements, direct-member grants |
| 17–20 tasks | Assign/reassign, fixed workflow, review approval, dependencies, dates, estimates/actuals, comments/files | Per-project workflows, time ledger, complete task history UI, capacity enforcement |
| Optional Bugs | Project toggle, severity, priority, assignment, status, comments/files | Linked-task UI, richer reproduction template and quality analytics |
| 21 assignments | Assign, submit, evaluate, score, feedback | Submission versioning and additional assessment workflows |
| 22–28 Agile | Sprints/lifecycle, goals/capacity, points, task board/backlog status, stand-ups, retrospectives | Stories, planning drag/drop, historical burndown/velocity, automatic carry-over, sprint-review workflow |
| 29–32 learning | Courses, lessons, assignment, text materials, uploads and progress | Rich topic hierarchy, video completion verification, material previews |
| 33 assessments | MCQ authoring/attempts, server scoring, passing score, max attempts | Timed sessions, coding sandbox, descriptive/project evaluation |
| 34–38 performance | Completion totals, workload, review forms, weight configuration | Combined scoring engine, quality/collaboration definitions, historical analytics |
| 39 workload | Unfinished hours across projects, weekly capacity reference | Date-aware availability, leave, overload enforcement |
| 40–41 skills | Employee skills and proficiency | Project requirements, gap calculations, learning recommendations |
| 42 notifications | In-app assignment/result notifications and read state; SMTP password recovery/confirmation and assignment/result emails | Other notification email, realtime delivery, durable queue/retry and scheduled reminders |
| 43 collaboration | Comments on major work entities | Threads, mentions and comment notifications |
| 44 files | Protected persistent uploads/downloads and metadata | Content scanning/MIME checks, object storage and version groups |
| 45 risks | Probability/impact/owner/mitigation/status CRUD | Computed severity and risk analytics |
| 51–52 reports | Scoped task CSV and printable report view | Native Excel/PDF, full employee/project/sprint reports and historical analytics |
| 53 search | Per-module search, project/status filters, server pagination | Global search, full filter matrix |
| 54 audit | Actor/action/target/time | Before/after changes, task history UI and retention |
| 55–59 architecture | Spring/JDBC, Flyway, indexed document store, dedicated account/file tables, named REST resources | Normalized domain tables, fully scoped SQL, OpenAPI |
| 60 security | Session/CSRF, RBAC/scope, prepared SQL, React escaping, upload authorization | Independent review, general rate limits, secret rotation, content scanning, JWT |
| 61–63 operations | Docker config, health endpoint, DB/upload volumes | Live PostgreSQL tests, load tests, backup/restore drills, monitoring, retention |
| 64–70 delivery | Builds, automated backend tests and documentation | Browser QA, UAT, production hosting and full Definition of Done |

## Next batches

1. Normalize/harden core data and APIs, complete authentication/session history, query optimization and task history.
2. Complete Agile planning, configurable workflows, dated capacity, sprint history and carry-over.
3. Complete attendance policies, richer learning and assessment workflows.
4. Implement skill gaps, scoring, dedicated dashboards and exports.
5. Connect notifications/background jobs; run PostgreSQL, browser, load and security testing; deploy and establish operations.

These are outstanding implementation tasks, not services represented as already connected.
