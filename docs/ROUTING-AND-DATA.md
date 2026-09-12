# Route-specific APIs and React Router

The former GET `/api/v1/state` and `/api/v1/entities/...` routes have been removed. Deploy the frontend and backend from this source package together; older clients using those URLs will receive 404.

## Browser navigation

React Router DOM 7.18.3 is pinned in the frontend manifest and lockfile (the npm stable release verified during implementation). `createBrowserRouter` and `RouterProvider` own application navigation. The existing Vinext server provides the HTML shell and same-origin Spring API proxy; it is not used as the application navigation router. The catch-all HTML page supports direct links and refreshes.

- `/login`, `/forgot-password`, `/reset-password`: authentication screens.
- `/change-password`: authenticated password screen.
- `/overview`: dashboard counts and task-status summary.
- `/<module>`: paginated list or board.
- `/<module>/new`: create form.
- `/<module>/:id`: individual record and workflow actions.
- `/<module>/:id/edit`: edit form.
- `/hierarchy`: paginated reporting levels.
- `/reports`, `/workload`: paginated employee delivery metrics.

Navigation uses React Router Link/NavLink and loaders. `useLoaderData` reads the current route's loader result; it does not receive all application tables. The authenticated layout's `/auth/session` response contains only the current user, role name and permission flags. These flags drive menu and action visibility; backend authorization is still mandatory.

Forms, progress changes, assignments, lesson completion, authentication, attendance and uploads use `useFetcher` with route actions. Actions call their corresponding Spring endpoint and React Router revalidates active loaders after successful submissions. Search, paging and board/list view are URL search parameters. Related comments/files and searchable reference fields use fetcher loaders. These client route loaders are not generic backend data endpoints.

Reference controls fetch only a page of `{id,title}` options for the relevant field. Their requests include the form's permission context and project when needed. Selected references can be resolved separately by ID. They never preload full users, roles or projects into every page. Unsaved create/edit forms use `useBlocker` before navigating away.

## Examples to check in the Network tab

| UI action | Backend request |
| --- | --- |
| Open Tasks | GET `/api/v1/tasks?page=0&size=25` (defaults may be omitted) |
| Switch to Projects | GET `/api/v1/projects` |
| Open a task | GET `/api/v1/tasks/:id` |
| Create a project | POST `/api/v1/projects` |
| Save edited project details | PUT `/api/v1/projects/:id` |
| Update task progress | PATCH `/api/v1/tasks/:id/progress` |
| Save role permissions | PUT `/api/v1/roles/:id/permissions` |
| Submit an assignment | POST `/api/v1/assignments/:id/submit` |
| Complete a lesson | POST `/api/v1/enrollments/:id/lessons/:lesson/complete` |
| Post a comment | POST `/api/v1/comments` |
| Upload an attachment | POST `/api/v1/files` |
| Log out | POST `/api/v1/auth/logout`, then navigate to `/login` |

The small session endpoint may also run during navigation/revalidation. Mutations first obtain the session CSRF token. Neither is an application-wide data load. Clicking a link to the exact current location may not cause a fresh load; changing module, filters or page runs the associated loader.

## Pagination and filtering

Module lists, lookup lists, file lists and workload reports return:

```json
{"items": [], "page": 0, "size": 25, "hasNext": false}
```

Pages are zero-based. The default size is 25, maximum 100. Lists support title search `q`, `status`, `projectId`, `ownerId`, and relevant relationship filters `entityKind`, `entityId`, `courseId`, `assessmentId`, `reportingTo`. Results are ordered by creation time and ID descending. Unauthorized rows are excluded before page boundaries; hidden records do not fill a page or appear in counts. A total count is intentionally omitted to avoid a full counting pass on each list request. Concurrent insertions can shift offset-based pages.

List responses omit heavy detail-only fields such as course lessons and assessment questions. Individual detail reads are authorized and sanitized, including removal of correct assessment answers for users without update permission. Related display labels do not include full referenced records or user emails.

## Backend structure and remaining storage limitations

Each module has a named controller with a fixed `/api/v1/<module>` base path. `ReadModuleEndpoints` and `ModuleEndpoints` share implementation without exposing a generic `/entities/{kind}` HTTP endpoint. Workflow controllers have explicit action mappings. See API-ENDPOINTS.md for the complete method/path inventory.

ReadService loads authorization relationships (users, roles, teams, projects) and scans only the requested module in bounded SQL batches. Search/status/project/owner filters are applied in SQL; existing reporting-tree policies are applied before returning a page. Relationship fields still live in JSON payloads, so those filters and some scope decisions are evaluated in Java. Business validations load the modules they need; deletion checks candidate references without constructing an application-wide snapshot.

This change removes bulk client payloads. It does not normalize the existing generic entity/JSON storage. Authorization relationship loading, large hierarchy evaluation, high-offset paging and summary scans still need optimization for large deployments. Full CSV exports intentionally export all authorized task rows. Existing data is preserved; no database reset is required.

## Running the update

1. Extract the new source package. Preserve your existing `.env` and data volumes.
2. From root: `docker compose up -d --build`.
3. From frontend: `pnpm install --frozen-lockfile`.
4. macOS/Linux: `SPRING_API_URL=http://localhost:8080 pnpm dev`.
5. Open `http://localhost:5173/login`, sign in, then inspect Network while changing modules.

Restart both frontend and backend. Opening a previous ZIP or an older hosted deployment will continue to show the old behaviour.

## Verification

39 Spring integration tests passed, including 14 focused resource API tests. Nine React Router data checks passed using the real router/loaders/actions and mocked HTTP. Run `node scripts/test-route-data.mjs` from `frontend/`. TypeScript and the production build passed. Browser automation could not run because the browser download was unavailable; live SMTP delivery and production PostgreSQL were not tested.
