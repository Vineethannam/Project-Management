# Verification

- React/TypeScript static type checking: passed.
- Vinext production frontend build: passed.
- Java 17 / Spring Boot backend compilation: passed.
- Spring integration suite: 17 tests passed, 0 failures, 0 errors.

Coverage: employee and reporting-manager visibility; project membership deduplication; reporting cycles; cross-project assignment rejection; employee approval/reassignment restrictions; valid review submission; stale-write rejection; membership removal with active work; disabled bug creation; referenced-role deletion; CSRF; unauthenticated reads; assessment answer-key filtering and server-side scoring; duplicate attendance check-in; rejection of self-approved task creation.

Tests use H2 PostgreSQL compatibility mode and MockMvc. No live PostgreSQL, browser, load or independent security certification is claimed. Optional WebMCP integration was not runtime-tested because no permitted supported browser context was available.

## Folder reorganization

Frontend moved to `frontend/`; type checking and the production build pass from the new layout. The root build shortcut stages the same Worker output for packaging. Frontend dependencies/lockfile and all backend source are unchanged. The earlier 17 backend test results still describe the unchanged backend; they were not rerun for this directory-only frontend change.
