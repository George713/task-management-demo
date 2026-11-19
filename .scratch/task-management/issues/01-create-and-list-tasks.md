# 01: Create and list Tasks (tracer bullet)

**What to build:** The first end-to-end path through the whole stack. A user opens the app in the browser, creates a Task with a name and an optional Priority, and sees all Tasks in the agreed order. Tasks are persisted in Postgres. This ticket also brings the project into existence: generated from code.quarkus.io (Quarkus 3 non-snapshot, Java 17, Maven, JAR packaging) with the Maven wrapper committed, the initial Flyway migration for the Task table, the Task entity with its creation rules, and the frontend's task-store interface with its API implementation only. See `spec.md` (Implementation Decisions: Platform, Backend structure, API contract, Schema, Frontend) and D1, D2, D4–D6, D8, D11–D13, D15, D16 in `DECISIONS.md`.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] Project generated from code.quarkus.io with Quarkus 3 (latest stable), Java 17, Maven, JAR packaging. Extensions limited to: REST + Jackson, Hibernate ORM with Panache, JDBC Postgres, Flyway, Hibernate Validator. Maven wrapper committed.
- [x] `./mvnw quarkus:dev` starts the app with Postgres provided automatically by Dev Services; no manual database setup.
- [x] The initial Flyway migration creates the Task table: generated id, `name` VARCHAR(255) NOT NULL, `done` BOOLEAN NOT NULL default false, `created` TIMESTAMPTZ NOT NULL, `priority` NOT NULL with a CHECK on LOW/NORMAL/URGENT. Hibernate schema generation is off and the mapping is validated against the schema at startup.
- [x] Priority is persisted as its name (text), never its ordinal.
- [x] Tasks can only be created through the entity's factory (`create(name, priority)`): it trims the name, defaults Priority to NORMAL, sets Created to now and Done to false. There is no way to set Priority or Created after creation.
- [x] `POST /api/tasks` with `{name, priority?}` returns 201 and the created Task as `{id, name, done, created, priority}`.
- [x] `POST` returns 400 with a readable message for a blank name, a name over 255 characters (after trimming), an unknown Priority, malformed JSON, or a body containing `created`, `done` or `id`.
- [x] `GET /api/tasks` returns all Tasks: open before Done, then URGENT → NORMAL → LOW, then newest Created first (sorted in the query, with an explicit Priority order rather than alphabetical).
- [x] One static English page served by Quarkus, styled with a pinned classless Pico.css: a create form (name input, Priority dropdown defaulting to NORMAL, Add button) above the Task list. Each row shows the name, a Priority badge and the Done state (read-only for now). API errors are shown inline. The list refreshes after a create.
- [x] The page reaches Tasks only through a task-store interface (list / create / update / remove; update and remove may be stubs until tickets 02 and 03). An API implementation uses the REST API, and a one-line config script set to `api` selects it.
- [x] `@QuarkusTest` + RestAssured tests against Dev Services Postgres cover: create with name only (defaults, trimming, Created set), create with explicit Priority, each create rejection above, and the list order across Done state, Priority and Created. Each test creates its own data through the API. `./mvnw test` passes.
- [x] Any new decision made along the way is appended to `DECISIONS.md`.

## Comments

**Resolved 2026-11-19** (merged into `feature/task-management`): branch `ticket/01-create-and-list` (5f6d768). 12 API tests passed; dev mode with Dev Services checked by hand. Done-in-list-order test deferred to 02 (D26) and added there. Decisions D19-D27.
