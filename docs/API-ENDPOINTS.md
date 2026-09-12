# API endpoint inventory

All paths have the `/api/v1` prefix. 175 method/path combinations across 30 REST controllers; inherited module methods count per concrete path. Actuator health is separate. Read-only/workflow modules do not expose generic writes.

Every protected request checks session validity and account status. CRUD permissions, reporting/project scope, optimistic versions and business rules apply. A listed mutation can still be rejected when a record must be retained or an action requires manager access. All non-GET requests require CSRF.

| Method | Path | Action |
| --- | --- | --- |
| GET | `/projects` | List (paginated) |
| GET | `/projects/options` | Reference options (paginated) |
| GET | `/projects/{id}` | Read details |
| POST | `/projects` | Create |
| PUT | `/projects/{id}` | Edit |
| DELETE | `/projects/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/tasks` | List (paginated) |
| GET | `/tasks/options` | Reference options (paginated) |
| GET | `/tasks/{id}` | Read details |
| POST | `/tasks` | Create |
| PUT | `/tasks/{id}` | Edit |
| DELETE | `/tasks/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/bugs` | List (paginated) |
| GET | `/bugs/options` | Reference options (paginated) |
| GET | `/bugs/{id}` | Read details |
| POST | `/bugs` | Create |
| PUT | `/bugs/{id}` | Edit |
| DELETE | `/bugs/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/sprints` | List (paginated) |
| GET | `/sprints/options` | Reference options (paginated) |
| GET | `/sprints/{id}` | Read details |
| POST | `/sprints` | Create |
| PUT | `/sprints/{id}` | Edit |
| DELETE | `/sprints/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/milestones` | List (paginated) |
| GET | `/milestones/options` | Reference options (paginated) |
| GET | `/milestones/{id}` | Read details |
| POST | `/milestones` | Create |
| PUT | `/milestones/{id}` | Edit |
| DELETE | `/milestones/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/risks` | List (paginated) |
| GET | `/risks/options` | Reference options (paginated) |
| GET | `/risks/{id}` | Read details |
| POST | `/risks` | Create |
| PUT | `/risks/{id}` | Edit |
| DELETE | `/risks/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/standups` | List (paginated) |
| GET | `/standups/options` | Reference options (paginated) |
| GET | `/standups/{id}` | Read details |
| POST | `/standups` | Create |
| PUT | `/standups/{id}` | Edit |
| DELETE | `/standups/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/retrospectives` | List (paginated) |
| GET | `/retrospectives/options` | Reference options (paginated) |
| GET | `/retrospectives/{id}` | Read details |
| POST | `/retrospectives` | Create |
| PUT | `/retrospectives/{id}` | Edit |
| DELETE | `/retrospectives/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/users` | List (paginated) |
| GET | `/users/options` | Reference options (paginated) |
| GET | `/users/{id}` | Read details |
| POST | `/users` | Create |
| PUT | `/users/{id}` | Edit |
| DELETE | `/users/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/teams` | List (paginated) |
| GET | `/teams/options` | Reference options (paginated) |
| GET | `/teams/{id}` | Read details |
| POST | `/teams` | Create |
| PUT | `/teams/{id}` | Edit |
| DELETE | `/teams/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/departments` | List (paginated) |
| GET | `/departments/options` | Reference options (paginated) |
| GET | `/departments/{id}` | Read details |
| POST | `/departments` | Create |
| PUT | `/departments/{id}` | Edit |
| DELETE | `/departments/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/roles` | List (paginated) |
| GET | `/roles/options` | Reference options (paginated) |
| GET | `/roles/{id}` | Read details |
| POST | `/roles` | Create |
| PUT | `/roles/{id}` | Edit |
| DELETE | `/roles/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/attendance` | List (paginated) |
| GET | `/attendance/options` | Reference options (paginated) |
| GET | `/attendance/{id}` | Read details |
| GET | `/assignments` | List (paginated) |
| GET | `/assignments/options` | Reference options (paginated) |
| GET | `/assignments/{id}` | Read details |
| POST | `/assignments` | Create |
| PUT | `/assignments/{id}` | Edit |
| DELETE | `/assignments/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/courses` | List (paginated) |
| GET | `/courses/options` | Reference options (paginated) |
| GET | `/courses/{id}` | Read details |
| POST | `/courses` | Create |
| PUT | `/courses/{id}` | Edit |
| DELETE | `/courses/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/enrollments` | List (paginated) |
| GET | `/enrollments/options` | Reference options (paginated) |
| GET | `/enrollments/{id}` | Read details |
| POST | `/enrollments` | Create |
| PUT | `/enrollments/{id}` | Edit |
| DELETE | `/enrollments/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/assessments` | List (paginated) |
| GET | `/assessments/options` | Reference options (paginated) |
| GET | `/assessments/{id}` | Read details |
| POST | `/assessments` | Create |
| PUT | `/assessments/{id}` | Edit |
| DELETE | `/assessments/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/attempts` | List (paginated) |
| GET | `/attempts/options` | Reference options (paginated) |
| GET | `/attempts/{id}` | Read details |
| GET | `/skills` | List (paginated) |
| GET | `/skills/options` | Reference options (paginated) |
| GET | `/skills/{id}` | Read details |
| POST | `/skills` | Create |
| PUT | `/skills/{id}` | Edit |
| DELETE | `/skills/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/reviews` | List (paginated) |
| GET | `/reviews/options` | Reference options (paginated) |
| GET | `/reviews/{id}` | Read details |
| POST | `/reviews` | Create |
| PUT | `/reviews/{id}` | Edit |
| DELETE | `/reviews/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/announcements` | List (paginated) |
| GET | `/announcements/options` | Reference options (paginated) |
| GET | `/announcements/{id}` | Read details |
| POST | `/announcements` | Create |
| PUT | `/announcements/{id}` | Edit |
| DELETE | `/announcements/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/comments` | List (paginated) |
| GET | `/comments/options` | Reference options (paginated) |
| GET | `/comments/{id}` | Read details |
| POST | `/comments` | Create |
| PUT | `/comments/{id}` | Edit |
| DELETE | `/comments/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/documents` | List (paginated) |
| GET | `/documents/options` | Reference options (paginated) |
| GET | `/documents/{id}` | Read details |
| POST | `/documents` | Create |
| PUT | `/documents/{id}` | Edit |
| DELETE | `/documents/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| GET | `/notifications` | List (paginated) |
| GET | `/notifications/options` | Reference options (paginated) |
| GET | `/notifications/{id}` | Read details |
| GET | `/audit` | List (paginated) |
| GET | `/audit/options` | Reference options (paginated) |
| GET | `/audit/{id}` | Read details |
| GET | `/settings` | List (paginated) |
| GET | `/settings/options` | Reference options (paginated) |
| GET | `/settings/{id}` | Read details |
| POST | `/settings` | Create |
| PUT | `/settings/{id}` | Edit |
| DELETE | `/settings/{id}?version=N` | Delete (business rules may require archive/deactivate instead) |
| PATCH | `/tasks/{id}/progress` | Update status / actual hours |
| PATCH | `/tasks/{id}/assignee` | Reassign |
| PATCH | `/bugs/{id}/progress` | Update status / actual hours |
| PATCH | `/bugs/{id}/assignee` | Reassign |
| PUT | `/roles/{id}/permissions` | Update permission matrix |
| PUT | `/teams/{id}/members` | Update team membership |
| PUT | `/projects/{id}/teams` | Update project teams |
| GET | `/attendance/current` | Own open attendance session |
| POST | `/attendance/check-in` | check-in |
| POST | `/attendance/check-out` | check-out |
| POST | `/attendance/break-start` | break-start |
| POST | `/attendance/break-end` | break-end |
| POST | `/assessments/{id}/submit` | Submit assessment answers |
| POST | `/assignments/{id}/submit` | Submit work |
| POST | `/assignments/{id}/evaluate` | Evaluate submission |
| POST | `/enrollments/{id}/lessons/{lesson}/complete` | Complete lesson |
| POST | `/notifications/{id}/read` | Mark read |
| GET | `/dashboard/summary` | Dashboard summary |
| GET | `/reports/workload` | Paginated employee metrics |
| GET | `/reports/tasks.csv` | Export authorized tasks |
| POST | `/files` | Upload multipart attachment |
| GET | `/files?entityKind=...&entityId=...` | Paginated attachment list |
| GET | `/files/{id}` | Download authorized attachment |
| GET | `/auth/csrf` | csrf |
| GET | `/auth/me` | me |
| GET | `/auth/session` | session |
| POST | `/auth/login` | login |
| POST | `/auth/logout` | logout |
| POST | `/auth/forgot-password` | forgot-password |
| POST | `/auth/reset-password` | reset-password |
| POST | `/auth/change-password` | change-password |
