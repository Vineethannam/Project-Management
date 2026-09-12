# Architecture

Browser → React/Vinext server → same-origin proxy → Spring Boot → PostgreSQL and upload volume.

## Authorization

Each user has one role and a nullable `reportingTo`. Roles store CRUD flags per submodule, grouped under modules. No moduleAll/subModuleAll/managementAll overrides. User scope is separately `hierarchy` or `organization`.

Requests resolve the active session user, check the current role permission, validate access to the old record, validate proposed relationships and business rules, then mutate and audit in one transaction. Employees see their assigned tasks in accessible projects. Reporting managers see transitive reports. Explicit project managers see all project tasks. Organization scope removes record filtering but does not bypass CRUD.

## Data model tradeoff

The alpha uses an indexed `entity` document table: kind, title, status, owner_id, project_id, JSON text payload, version, timestamps. Accounts and uploaded-file metadata are separate relational tables. Relationships inside JSON are service-validated; they are not database foreign keys or normalized join tables.

This deliberately differs from the SRS's fully normalized table proposal. Membership derives from team member IDs plus project managers, with duplicates removed. A database coordination-row lock protects multi-record validations; an optimistic version rejects stale edits.

The snapshot endpoint has been removed. Named resource APIs return authorized, paginated records; React Router loaders request data when routes change and fetchers perform mutations. See ROUTING-AND-DATA.md for the request contract and remaining query limits. Before larger deployments, introduce normalized domain tables, foreign-key constraints, fully scoped SQL, per-aggregate locking and historical read models. The SRS scale and latency targets are not verified.

## Files

Files use generated storage keys under the configured upload directory. Protected downloads recheck Documents permission and parent-record visibility. Limit: 10 MiB with an extension allowlist. Downloads use attachment disposition and a generic binary media type. Transaction rollback removes newly uploaded bytes. Content scanning, MIME verification, object storage and grouped version history remain work.

## Authentication

Server sessions with CSRF are implemented. JWT/refresh tokens are not. API permissions are checked in services/controllers; the Spring filter chain provides CSRF and security headers. Unauthenticated application calls are rejected by the session resolver. The public health endpoint exposes only health status.

## Audit and tests

Audit records track actor, action, target and time. Full before/after snapshots are pending. Integration tests exercise Spring, JDBC/H2 and HTTP endpoints. React Router navigation and fetcher tests exercise the actual loaders/actions with mocked HTTP. Browser QA remains unavailable in this environment.
