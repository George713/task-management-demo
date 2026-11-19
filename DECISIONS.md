# Decision Log

A running record of every decision made while building this project for the Fairsource interview task (`Fairsource_Interview-Task.pdf`): big and small, including the reasoning and the alternatives that were rejected. Read top to bottom, it tells the story of how the project came together.

The bigger, hard-to-reverse decisions also have their own ADR in [`docs/adr/`](docs/adr/). Domain vocabulary lives in [`GLOSSARY.md`](GLOSSARY.md).

## Fixed by the brief (not our decisions)

The task PDF prescribes these, so they are constraints, not choices:

- Spring Boot 3 **or** Quarkus 3 (no snapshot), generated via start.spring.io / code.quarkus.io
- Maven, Java 17, JAR packaging
- Tasks persisted in **Postgres**
- Domain: `Task` (name, done, created: `Instant`, priority) and `Priority` (LOW, NORMAL, URGENT)
- Use cases: create task, change task (name / done), delete task, through a simple browser UI (English or German)
- Runnable directly with Java **or** as a Docker container; Postgres may run in Docker
- Where the brief is unclear: decide and justify

---

## Session 1: Grilling (2026-11-19)

### D1. Framework: Quarkus 3
**Decision:** Quarkus 3 on Java 17 with Maven, generated from code.quarkus.io.
**Alternatives:** Spring Boot 3, which has more tutorials and more examples in AI training data.
**Why:** Quarkus dev mode starts a Postgres container on its own ("Dev Services"), so local setup needs zero configuration. It also starts quickly and builds small container images. And it fits the reviewer's ecosystem: Sebastian Daschner is well known in the Quarkus / Jakarta EE community.
→ [ADR-0001](docs/adr/0001-quarkus-over-spring-boot.md)

### D2. Frontend: JSON REST API + one plain HTML/JS page
**Decision:** The backend exposes a JSON REST API for tasks. The UI is one static `index.html` with plain JavaScript (no framework, no npm build), served by the Quarkus app itself.
**Alternatives:** Server-side templates (Qute) + htmx; a React/Vue single-page app.
**Why:** The API is a clean seam between UI and backend: it can be tested on its own and it is easy to explain. A JS framework with a build pipeline is overkill for three use cases.
→ [ADR-0002](docs/adr/0002-rest-api-with-plain-js-frontend.md)

### D3. A browser-only demo version, in addition to the real app
**Decision:** Besides the real Quarkus + Postgres app, also provide a version of the UI that runs entirely in the browser (no backend) and is hosted from the repo, so reviewers can try it without setting anything up.
**Alternatives:** Only the real app, made runnable via `docker compose up`.
**Why:** It lowers the barrier for a first look. The real app remains the actual submission. Shape: see D8–D10.

### D4. Priority is set at creation and never changes
**Decision:** Only `name` and `done` can be changed after creation. `priority` is chosen once, when the task is created.
**Alternatives:** Allow editing priority too, as an extension of the brief.
**Why:** The brief literally says "Task ändern (name, bzw. done)". Following it to the letter shows close reading, and it means less to build.

### D5. Rules for a new task
**Decision:** The user provides `name` and `priority` (default NORMAL). The server sets `created`; clients cannot send it. `done` always starts as false. `name` is required, not blank, trimmed and at most 255 characters.
**Why:** Reasonable defaults the brief leaves open. `created` comes from the server because the client's clock and input can't be trusted.

### D6. Schema managed by Flyway migrations
**Decision:** Versioned SQL scripts (`V1__create_task.sql`, …), applied by Flyway at startup.
**Alternatives:** Let Hibernate generate the schema from the entity classes (`drop-and-create` / `update`).
**Why:** Hibernate generation is fine for prototypes but not for production:
- `drop-and-create` wipes all data on every start.
- `update` only *adds* things. It never removes or renames columns (a renamed field becomes a new empty column, and the old data is orphaned), and it can't migrate data or tighten constraints safely.
- Schema changes don't appear as reviewable code, there is no history of what changed when, and environments drift apart without anyone noticing.

Flyway keeps every schema change as a reviewed, versioned SQL file. It records what has been applied in a `flyway_schema_history` table and applies exactly the same steps in every environment. It costs one small SQL file here.

### D7. How decisions are documented
**Decision:** Three layers: this `DECISIONS.md` covers the whole process (every decision, big or small, in order). ADRs in `docs/adr/` are only for decisions that are hard to reverse, surprising and the result of a real trade-off. `GLOSSARY.md` holds domain vocabulary only.
**Why:** The interview will be about the reasoning, so the whole journey should be readable in one place, with the heavyweight decisions also standing alone.

### D8. One frontend, two swappable task stores
**Decision:** The single `index.html` reaches tasks only through a small task-store interface with two implementations: `ApiTaskStore` (calls the REST API) and `LocalTaskStore` (saves in the browser). A one-line `config.js` picks the implementation. The Quarkus app ships it set to `api`; the GitHub Pages deploy swaps it to `local`.
**Alternatives:** A separate demo HTML file (two UIs that drift apart); detecting automatically whether an API is reachable (hidden magic).
**Why:** One UI, no duplication, and an explicit switch. This is the ports-and-adapters idea on a small scale, and it is the payoff of having a separate API (D2).
→ [ADR-0003](docs/adr/0003-swappable-task-store-for-browser-demo.md)

### D9. Demo data lives in `localStorage`
**Decision:** The browser-only demo stores tasks in `localStorage`, so they survive a page reload. A banner says "Demo mode: tasks are stored in your browser only, not in Postgres", and a reset button clears them.
**Alternatives:** `sessionStorage`, which clears when the tab closes.

### D10. Public GitHub repo with a GitHub Pages demo
**Decision:** The repo is public and the browser demo is hosted on GitHub Pages. The submission is the repo link.
**Alternatives:** A private repo with an invite to `@sdaschner`. On a free account Pages only works for public repos, so this would need GitHub Pro or no hosted demo.

### D11. Backend structure: two classes, with the rules inside the entity
**Decision:**
- `TaskResource` handles HTTP for every `/api/tasks` endpoint. It turns JSON into request objects, checks them (`@NotBlank`, `@Size`) and runs each request in a transaction.
- `Task` is the entity (one object = one row) and it enforces its own rules. New tasks are only made through `Task.create(name, priority)`, which sets `created` and `done = false`. Changes only go through `rename(...)` and `markDone(...)`. There is no way to change `priority` or `created`.
- Database access uses Panache's built-in methods. There is no separate repository or service class.

**Alternatives:** A three-layer Resource → Service → Repository design; an "anemic" entity with public setters and the rules somewhere else.
**Why:** No use case coordinates several things, so a service layer would only pass calls through. Putting the rules on the entity (a "rich domain model") means no code path can bypass them. A service layer is the next step if multi-step use cases appear.

**Rules enforced at three layers (defence in depth):**
- **Postgres (Flyway SQL)** holds the hard data rules: `NOT NULL`, `VARCHAR(255)`, a `CHECK` on the priority values. They hold even for direct SQL access.
- **The `Task` entity** holds the behaviour rules: priority never changes, the server sets `created`, names are trimmed.
- **Request validation** turns bad input into clear 400 errors.

Priority is stored as text (`'URGENT'`), not as a number, so reordering the list of values in Java can't silently corrupt data. Hibernate checks the entity against the real schema at startup, so the two can't drift apart unnoticed.

### D12. REST API shape
**Decision:**
- `GET /api/tasks` lists all tasks.
- `POST /api/tasks` with `{name, priority?}` returns 201.
- `PATCH /api/tasks/{id}` with `{name?, done?}` returns 200.
- `DELETE /api/tasks/{id}` returns 204.
- 400 for invalid input, 404 for an unknown id. IDs are numeric and generated by Postgres.

**Fields that aren't allowed are rejected, not ignored:** a PATCH body containing `priority` or `created`, or a POST body containing `created`, `done` or `id`, gets a 400. A client trying to break a rule should learn about it, not have its input silently dropped. *(Made precise while writing the spec.)*

**Why PATCH over PUT:** a change touches only one or two fields (name and/or done). PUT would imply replacing the whole Task, including fields that must never change.

### D13. List order
**Decision:** Open tasks first, then by priority (URGENT → NORMAL → LOW), then newest first. The server sorts.

### D14. Running it: `docker compose up --build` needs only Docker
**Decision:** A multi-stage Dockerfile builds the JAR with Maven inside the container, so a reviewer needs no Java. Compose starts Postgres and the app. The README also documents the plain-Java route the brief asks for: `docker compose up db`, then `./mvnw package`, then `java -jar target/quarkus-app/quarkus-run.jar`. The Maven wrapper is included. Developers use `./mvnw quarkus:dev`, which starts Postgres via Dev Services.

### D15. Tests: API-level tests against real Postgres
**Decision:** A handful of `@QuarkusTest` + RestAssured tests. They cover create, change and delete, and the rule violations: blank name, unknown id, and attempts to change priority or set `created`. They run against a real Postgres started automatically. No frontend tests and no mock-heavy unit tests.
**Why:** Tests at the HTTP boundary check what the reviewer actually sees, survive internal refactoring, and use the same database as production.

### D16. UI: English, one page, classless CSS
**Decision:** One English page. A create form at the top (name, priority dropdown, Add). Below it the list: done checkbox, click-to-edit name, priority badge, delete button. API errors are shown inline. Styling comes from a single classless stylesheet (Pico.css).
**Why:** The brief asks for a "simple" UI. Classless CSS looks decent at zero effort.

### D17. Build in vertical slices tracked as local issues
**Decision:** After the grilling: write a spec, then split it into issues under `.scratch/task-management/` (per CLAUDE.md). Each issue is a vertical slice delivering one working feature end to end (API, test, UI): scaffold → create & list → change → delete → browser demo + Pages → README.
**Why:** Each slice is runnable and reviewable on its own, and agents get small, well-defined tasks.

### D18. Be transparent about the agentic workflow
**Decision:** The workflow is documented openly (see "How this was built" below).
**Why:** The author directed the requirements and the architecture; agents wrote most of the Java. Saying so plainly is stronger than hiding it, and this log is the evidence of who made the decisions.

---

## How this was built

1. **Grilling session.** Every open question was worked through as a decision tree. The answers are recorded in this file, in the ADRs and in the glossary.
2. **Spec.** The settled decisions were written into one spec.
3. **Issue slicing.** The spec was split into vertical-slice issues under `.scratch/`.
4. **Test-driven implementation by agents.** Each issue was implemented test-first (red → green → refactor).
5. **Review at the end.** The finished project was reviewed against the spec and these decisions.
