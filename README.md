# ProjectSphere — development alpha

React/TypeScript + Spring Boot 3 / Java 17 implementation of the core ProjectSphere workflows. **The entire SRS and its production gates are not finished.** See [SRS coverage](docs/SRS-COVERAGE.md).

## Folder layout

| Path | Contents |
|---|---|
| `frontend/` | All React UI, frontend server proxy routes, frontend dependencies, lockfile and build configuration |
| `backend/` | Spring Boot Java source, Maven dependencies, database migrations and upload handling |
| `docs/` | Shared API, architecture, verification and coverage documentation |
| `compose.yml` | Starts PostgreSQL and the backend together |
| `.env.example` | Root Docker/database/backend environment template |
| `frontend/.env.example` | Frontend server connection template; no database or storage secrets |
| `package.json` at root | Optional command shortcuts only; no frontend dependencies |

Keep the whole extracted folder together. The frontend build reads the shared hosting manifest from the repository root.

## Delivered functionality

Custom roles and CRUD matrix; reporting hierarchy; users/departments/teams; projects and team-derived membership; project-bound tasks/optional bugs; sprint records; milestones/risks; stand-ups/retrospectives; courses/lessons/enrollments; MCQ assessments; assignments/submission/evaluation; explicit attendance/break tracking; comments; protected uploads; in-app notifications; audit events; overview/workload reports and task CSV export.

## Real accounts only

The frontend requires `SPRING_API_URL`. Every workspace operation uses the Spring Boot backend and database. There is no demo login, sample user selector, or browser-only data storage. Missing backend configuration displays the login screen with an unavailable-service message.

A Java/container host and database are required for a deployed installation. No external Java host has been provisioned here.

## Local backend

Requirements: Java 17+, Maven 3.9+. From `backend/`, set `BOOTSTRAP_EMAIL` to your email and `BOOTSTRAP_PASSWORD` to a unique value of at least 12 characters, then run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile uses durable H2 storage in `backend/data/` with PostgreSQL compatibility mode. First startup creates only default roles, settings and your administrator account; it does not create sample projects, tasks or employees.

## PostgreSQL deployment

Copy `.env.example` to `.env`, fill all database and bootstrap values, then run:

```bash
docker compose up --build
```

The default profile seeds only roles, settings and the initial administrator. `BOOTSTRAP_EMAIL` and `BOOTSTRAP_PASSWORD` are required on an empty database. Bootstrap never overwrites an existing database. PostgreSQL and uploads have persistent volumes. The API is bound to loopback. Place HTTPS in front of it for deployment. Set `COOKIE_SECURE=false` only for local HTTP development.

The PostgreSQL container deployment has not been exercised here; the integration tests use H2 PostgreSQL mode.

## Frontend

Requirements: Node 22.13+ and pnpm 11.19.0, pinned in `frontend/package.json`. Install dependencies inside `frontend/`, not the repository root.

```bash
cd frontend
pnpm install --frozen-lockfile
```

Start the connected frontend on macOS/Linux:

```bash
SPRING_API_URL=http://localhost:8080 pnpm dev
```

Or in Windows PowerShell:

```powershell
$env:SPRING_API_URL = "http://localhost:8080"
pnpm dev
```

Open `http://localhost:5173/login`. Keep `SPRING_API_URL` set to connect to the backend.

The explicit environment commands above avoid relying on automatic dotenv loading. `frontend/.env.example` documents the frontend setting. Keep database passwords in the root Docker `.env` or the Java process environment; never put them into browser-public variables. Docker Compose reads the root `.env`; a separately launched Java process does not.

Runtime environment values on the hosted Site are configured separately. API calls stay on the frontend origin; CORS exceptions are not needed. After installing frontend dependencies, the root `pnpm dev` and `pnpm build` commands are optional shortcuts. The root build stages output for Sites packaging.

## Verification

From the project root:

```bash
cd frontend
pnpm exec tsc --noEmit
pnpm build
cd ../backend
mvn test
```

The backend suite uses real Spring services, H2 and MockMvc. It covers visibility, assignment rules, hierarchy cycles, stale updates, CSRF, assessment scoring and attendance concurrency. Browser testing, live PostgreSQL testing and load testing remain separate gates.

## Security contract

This alpha uses HttpOnly server sessions and Spring CSRF protection, **not yet the SRS's JWT/refresh-token contract**. Passwords use BCrypt. Five failed attempts trigger account lockout. Logout invalidates the session. Inactive accounts are rejected on every protected operation. Recovery emails, SSO and MFA remain pending.

Roles control CRUD; user scope controls record visibility. Role names never grant access. Hierarchy visibility does not grant write permissions. Organization-wide scope is separately assigned and requires Users write access plus existing organization scope. A user cannot remove their own administrative access through the edit form.

## Source map

- `frontend/components/projectsphere/workspace.tsx`: screens, gates, editor and detail workflows.
- `frontend/lib/projectsphere/`: model and form fields.
- `frontend/app/api/v1/[...path]/route.ts`: same-origin Spring proxy.
- `backend/src/main/java/com/projectsphere/Policy.java`: authoritative visibility rules.
- `backend/src/main/java/com/projectsphere/WorkspaceService.java`: transactions and validation.
- `backend/src/main/resources/db/migration/`: Flyway schema.
- `docs/`: architecture, endpoints and SRS coverage.

## Where uploads are stored

With Docker, files are at `/app/data/uploads` inside the backend container, backed by the named `uploads` volume. Running Java from `backend/` stores files at `backend/data/uploads` by default. The frontend `public/` folder is only for static UI assets. S3 is not yet integrated.

On macOS M1 or later, Windows and Linux, container paths are the same. To see uploads in Finder/File Explorer, replace the backend volume mapping with `./local-uploads:/app/data/uploads` and create that host directory first. Existing named-volume files are not copied automatically. The container user needs write permission to the host folder.

The frontend still contains unused `db/`, `drizzle/`, and `examples/` starter files. They are not the application database; the actual database connection and migrations are under `backend/`.

The updated source includes Logout, Login, Change password, Forgot password and Reset password. Updating a downloaded source package does not deploy the changes to an existing hosted installation.

## Password recovery and Gmail

Login, forgot-password, reset-password and authenticated change-password screens are included. Logout is available in the top bar and sidebar. Fill the commented `MAIL_*` and `FRONTEND_URL` settings in root `.env`; see [password and email setup](docs/PASSWORD-AND-EMAIL-SETUP.md) for instructions, APIs, testing and delivery limitations.

## React Router and separate APIs

The frontend now uses React Router DOM 7.18.3, `useLoaderData` and `useFetcher`. Module pages request only their paginated data; the bulk `/state` endpoint is removed. See [routing and data guide](docs/ROUTING-AND-DATA.md) and [full API inventory](docs/API-ENDPOINTS.md). Restart both applications after updating.
