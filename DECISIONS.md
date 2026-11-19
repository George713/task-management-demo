# Decision Log

A running record of every decision made while building this project for an interview task (the task PDF, which is not part of this repo): big and small, including the reasoning and the alternatives that were rejected. Read top to bottom, it tells the story of how the project came together.

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
**Why:** Quarkus dev mode starts a Postgres container on its own ("Dev Services"), so local setup needs zero configuration. It also starts quickly and builds small container images. And it fits the reviewers' ecosystem, which is close to the Quarkus / Jakarta EE community.
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
**Alternatives:** A private repo with an invite to the reviewer. On a free account Pages only works for public repos, so this would need GitHub Pro or no hosted demo.

### D11. Backend structure: two classes, with the rules inside the entity
**Decision:**
- `TaskResource` handles HTTP for every `/api/tasks` endpoint. It turns JSON into request objects, checks them (`@NotBlank`, `@Size`) and runs each changing request (POST, PATCH, DELETE) in a transaction. *(Originally "each request"; the read-only `GET` needs no transaction and doesn't have one.)*
- `Task` is the entity (one object = one row) and it enforces its own rules. New tasks are only made through `Task.create(name, priority)`, which sets `created` and `done = false`. Changes only go through `rename(...)` and `markDone(...)`. There is no way to change `priority` or `created`.
- Database access uses Panache's built-in methods. There is no separate repository or service class.

*(It ended up as three classes, not two: `ErrorMappers` turns rejected requests into the readable `{"message": "..."}` body (D22, D29). It holds no rules, only HTTP error mapping. After the code review there are also two small helpers next to `TaskResource`: the `@TaskName` constraint and `StrictJsonTypes` (D47). The structure is otherwise as described: no service or repository layer, and the rules stay in `Task`.)*

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
**Decision:** A multi-stage Dockerfile builds the JAR with Maven inside the container, so a reviewer needs no Java. Compose starts Postgres and the app. The README also documents the plain-Java route the brief asks for: `docker compose up db`, then `./mvnw package`, then `java -jar target/quarkus-app/quarkus-run.jar`. The Maven wrapper is included. Developers use `./mvnw quarkus:dev`, which starts Postgres via Dev Services. *(The README uses `docker compose up -d db`, so the JAR can run in the same terminal; see D44. The Docker setup itself is D32–D35.)*

### D15. Tests: API-level tests against real Postgres
**Decision:** A handful of `@QuarkusTest` + RestAssured tests. They cover create, change and delete, and the rule violations: blank name, unknown id, and attempts to change priority or set `created`. They run against a real Postgres started automatically. No frontend tests and no mock-heavy unit tests. *(It ended up as 26 tests in four classes, one per use case: create, list, change, delete. Still all at the HTTP API. After the code review it is 34 tests, and the four classes share their HTTP calls through one small `TaskApi` helper.)*
**Why:** Tests at the HTTP boundary check what the reviewer actually sees, survive internal refactoring, and use the same database as production.

### D16. UI: English, one page, classless CSS
**Decision:** One English page. A create form at the top (name, priority dropdown, Add). Below it the list: done checkbox, click-to-edit name, priority badge, delete button. API errors are shown inline. Styling comes from a single classless stylesheet (Pico.css).
**Why:** The brief asks for a "simple" UI. Classless CSS looks decent at zero effort.

### D17. Build in vertical slices tracked as local issues
**Decision:** After the grilling: write a spec, then split it into issues under `.scratch/task-management/` (per CLAUDE.md). Each issue is a vertical slice delivering one working feature end to end (API, test, UI): scaffold → create & list → change → delete → browser demo + Pages → README. *(The actual tickets: 01 scaffold + create & list as one tracer bullet, 02 change, 03 delete, 04 Docker Compose and plain Java, 05 browser demo + Pages, 06 README. Running the app got its own slice instead of hiding inside the scaffold.)*
**Why:** Each slice is runnable and reviewable on its own, and agents get small, well-defined tasks.

### D18. Be transparent about the agentic workflow
**Decision:** The workflow is documented openly (see "How this was built" below).
**Why:** The author directed the requirements and the architecture; agents wrote most of the Java. Saying so plainly is stronger than hiding it, and this log is the evidence of who made the decisions.

---

## Implementation: ticket 01, create and list (2026-11-19)

### D19. Quarkus 3.40 LTS, JAR packaging set explicitly
**Decision:** Generated from code.quarkus.io on the recommended stream, Quarkus 3.40.1.2 (an LTS release), with group `com.example`, artifact `task-management`, Java package `com.example.tasks`. code.quarkus.io now generates `<packaging>quarkus</packaging>`; we changed it to `<packaging>jar</packaging>` and bound the Quarkus plugin goals (`build`, `generate-code`, `generate-code-tests`) explicitly.
**Why:** The brief requires JAR packaging, and `jar` says so literally in the POM. The build output is unchanged (`target/quarkus-app/quarkus-run.jar`). The generated `src/main/docker/` files and `.dockerignore` were kept untouched for now; ticket 04 owns the Docker setup.

### D20. Postgres generates the id; the entity has no public fields
**Decision:** `Task` extends Panache's `PanacheEntityBase` and declares its own `id` as an `IDENTITY` column (`BIGINT GENERATED ALWAYS AS IDENTITY`). All fields are private, with getters only.
**Alternatives:** `PanacheEntity`, which brings a public `id` backed by a Hibernate sequence (`task_seq`, allocation size 50) and encourages public fields.
**Why:** Public fields would let any code assign `priority` or `created`, which breaks D4/D5. An identity column means the database generates the id (D12) without an extra sequence in the migration.

### D21. Request bodies refuse unknown fields globally
**Decision:** `quarkus.jackson.fail-on-unknown-properties=true`, so any field a request object doesn't declare is a 400.
**Alternatives:** Per-class `@JsonIgnoreProperties(ignoreUnknown = false)`. This doesn't work: `false` is the annotation's default, so Jackson falls back to the global setting, which Quarkus sets to "ignore".
**Why:** It implements D12 ("rejected, not ignored") in one place. The server only deserializes request objects, so nothing else is affected.

### D22. One error shape: `{"message": "..."}`
**Decision:** Every 400 carries a JSON body with one readable `message`. Our own exception mappers replace Quarkus's defaults for validation errors, unknown or mistyped fields, unknown Priority values ("Invalid priority 'HIGH'. Allowed values: LOW, NORMAL, URGENT") and malformed JSON. Without them, Quarkus returns parser internals (line and column numbers) or an empty body.
**Why:** The page shows `message` inline as-is (D16), and any API client gets the same predictable shape.

### D23. The name is trimmed before it is validated
**Decision:** The request object trims the name (`String.strip()`, Unicode whitespace) before `@NotBlank`/`@Size(max = 255)` run. The entity factory trims again. *(After the code review the trim follows JavaScript's `trim()` instead of `String.strip()`, see D46, and both request objects share the rules through `@TaskName`.)*
**Why:** The 255-character limit applies to the stored name. A 255-character name with surrounding spaces should be accepted. The second trim in the entity keeps the rule true for any future caller (D11).

### D24. Created is stored at microsecond precision
**Decision:** `Task.create` truncates `Instant.now()` to microseconds.
**Why:** Postgres `TIMESTAMPTZ` keeps microseconds, but Java's clock is finer. Without truncation, the Task returned by `POST` had a different `created` than the same Task returned by `GET` (the database rounds it). A test pins this down.

### D25. Exact ties in the list order fall back to newest id
**Decision:** The list query sorts by `done`, an explicit `CASE` on Priority (URGENT, NORMAL, LOW), `created desc`, and finally `id desc`.
**Why:** It makes the order fully deterministic even if two Tasks share the same microsecond.

### D26. The Done part of the list order is tested in ticket 02
**Decision:** Ticket 01 tests the order by Priority and by Created. The query already puts open Tasks before Done ones, but its test comes with ticket 02.
**Why:** Tests only use the HTTP API (D15), and a Done Task can't be made through the API until `PATCH` exists. Writing it into the database directly would bypass the seam. Ticket 02 already lists "a Done Task moves below open Tasks" as a test.

### D27. Frontend files and pinned stylesheet
**Decision:** The page lives in `src/main/resources/META-INF/resources/`: `index.html`, `app.js` (UI), `task-store.js` (interface and factory), `api-task-store.js`, and the one-line `config.js` (`export const TASK_STORE = 'api';`). Pico.css 2.1.1 (classless build) is loaded from jsDelivr. Priority badges are `<mark>` elements coloured by a few lines of inline CSS. *(Ticket 01 also had `update`/`remove` reject with "not available yet"; tickets 02 and 03 replaced those placeholders, and ticket 05 added `local-task-store.js`, see D38.)*

### D28. PATCH treats a null field like a missing one; no body is an empty PATCH
**Decision:** In `PATCH /api/tasks/{id}`, `"name": null` or `"done": null` means "leave unchanged", same as leaving the field out. A request with no body at all is handled like `{}`: 200 with the unchanged Task. A name that is sent is trimmed, then must be 1 to 255 characters.
**Why:** It is the simplest reading of "fields left out stay unchanged". Neither field can ever be null in a Task, so a null can't mean "clear it".

### D29. A 404 uses the same `{"message": "..."}` body as a 400
**Decision:** Changing an unknown id returns 404 with a message like "Task 42 was not found. It may have been deleted."
**Why:** The page shows `message` inline (D16, D22). A Task deleted in another tab then gets an explanation instead of a bare status code.

### D30. Priority or Created in a PATCH gets the generic unknown-field message
**Decision:** The PATCH request object declares only `name` and `done`. A `priority` or `created` field is refused by the global unknown-field rule (D21): "Field 'priority' is not allowed. Allowed fields: done, name".
**Alternatives:** A special message such as "Priority can't be changed after creation".
**Why:** It needs no extra code, and the message already says which fields are allowed.

### D31. Inline rename: Enter saves, Escape or leaving the field cancels
**Decision:** Clicking a name (or pressing Enter on it) swaps it for an input. Enter saves; Escape or clicking elsewhere cancels without saving. If saving fails, the input stays open with the message below it. A failed Done toggle reloads the list, so the checkbox shows the real state again, and shows the message above the list.
**Why:** The spec defines only Enter and Escape. Cancelling on blur means a change is saved only on an explicit Enter.

---

## Implementation: ticket 04, Docker Compose and plain Java (2026-11-19)

### D32. One root `Dockerfile` and `compose.yaml`; the generated Dockerfiles are gone
**Decision:** A multi-stage `Dockerfile` at the repo root (build: `eclipse-temurin:17-jdk` + Maven wrapper; runtime: `eclipse-temurin:17-jre-alpine`, non-root user, `java -jar quarkus-run.jar`) and a `compose.yaml` with a fixed project name `task-management`. The four generated files under `src/main/docker/` are deleted. `.dockerignore` now excludes `target/`, `.git` and docs instead of allowing only `target/`.
**Alternatives:** Keep the generated `Dockerfile.jvm` (UBI 9 OpenJDK runtime). It copies a JAR built on the host, so the reviewer would need Java, which breaks D14.
**Why:** The root files are what `docker compose up --build` finds by default. The fixed project name gives the data volume the same name (`task-management_db-data`) whatever the checkout folder is called.

### D33. The Docker build skips the tests
**Decision:** The build stage runs `./mvnw package -DskipTests`.
**Why:** The tests need Docker for Dev Services, which a build container doesn't have. They run with `./mvnw test`, and in the plain-Java route `./mvnw package` runs them too.

### D34. The wrapper's checksum is protected from Windows line endings and missing `unzip`
**Decision:** `.gitattributes` forces LF for `.mvn/wrapper/maven-wrapper.properties`, and the build stage installs `unzip`.
**Why:** Both broke the build with "Failed to validate Maven distribution SHA-256". With `core.autocrlf=true`, the checksum line ends in `\r`. Without `unzip`, the wrapper downloads the `.tar.gz` while the pinned checksum is for the `.zip`. Dropping the checksum would have hidden the problem instead of fixing it.

### D35. Dev Services use the same Postgres image as Compose
**Decision:** `quarkus.datasource.devservices.image-name=postgres:16-alpine`.
**Why:** Quarkus 3.40 Dev Services default to `postgres:18`. D15 says the tests use the same database as production, so the version has to match too.

---

## Implementation: ticket 03, delete a Task (2026-11-19)

### D36. Deleting a Task that is already gone is a 404, not a silent 204
**Decision:** `DELETE /api/tasks/{id}` returns 204 only if a Task was actually removed. Deleting an unknown id, including one that was just deleted, returns 404 with the same `{"message": "..."}` body as a PATCH on an unknown id (D29).
**Alternatives:** An idempotent DELETE that answers 204 whether or not the Task existed.
**Why:** The spec asks for 404 on an unknown id (D12, user story 30), and the page can then tell the user that the Task was already deleted elsewhere (user story 23). A test pins down that a second delete of the same Task gets 404.

### D37. The delete button deletes at once, without a confirmation dialog
**Decision:** Each row has a "Delete" button. Clicking it deletes the Task straight away and reloads the list. If the delete fails (for example a 404 because another tab already deleted it), the list is reloaded and the message is shown above it, the same as a failed Done toggle (D31).
**Alternatives:** A browser `confirm()` or an undo option.
**Why:** The spec asks for a delete button and nothing more, and undo is out of scope. A confirmation step would be UI behaviour nobody asked for.

---

## Implementation: ticket 05, browser demo and GitHub Pages (2026-11-19)

### D38. The local store copies the server's messages word for word, and checks in the same order
**Decision:** `local-task-store.js` rejects with the exact texts the API sends ("Name must not be blank", "Name must be at most 255 characters", "Invalid priority 'HIGH'. Allowed values: LOW, NORMAL, URGENT", "Field 'priority' is not allowed. Allowed fields: done, name", "Task 3 was not found. It may have been deleted."). Its checks run in the server's order: request shape first (Priority, unknown fields), then the name, then whether the Task exists. Ids count up from 1, and a failed create uses no id. Created is `new Date().toISOString()` (millisecond precision, still ISO-8601 UTC). Ties in the sort fall back to newest id, the same as D25.
**Alternatives:** Generic demo messages such as "Invalid name".
**Why:** User story 41 asks for a faithful preview, and the page shows the message as-is (D22). Matching the texts means the demo looks exactly like the real app, including its error states.

### D39. Stored as one JSON entry: `{nextId, tasks}`
**Decision:** All demo Tasks live under one `localStorage` key, `task-management-demo.tasks`, as `{"nextId": n, "tasks": [...]}`. Every operation reads the entry, changes it and writes it back. Reset removes the key, so ids start again from 1. If the entry can't be parsed, or storage is blocked, the store rejects with a readable message that points to the "Reset demo" button, and the page shows it like any other error.
**Alternatives:** One key per Task; an id counter derived from the highest stored id.
**Why:** It is the smallest format that keeps ids unique after deletes. A deleted id is never reused, the same as the Postgres identity column. Read-modify-write on a few dozen Tasks costs nothing. Two tabs writing at the same moment could lose a change, which is acceptable because cross-tab sync is out of scope.

### D40. The page shows demo mode only when the store can be reset
**Decision:** `reset()` is an extra operation that only the local store offers (documented in `task-store.js`). `app.js` shows the banner ("Demo mode: tasks are stored in your browser only, not in Postgres") and the "Reset demo" button if, and only if, the store has `reset`. The banner sits in `index.html`, hidden by default. Reset deletes immediately, without a confirmation, like Delete (D37).
**Alternatives:** `app.js` reads `TASK_STORE === 'local'` itself; a separate `isDemo` flag.
**Why:** The UI keeps talking only to the store (D8). Switching `config.js` is the one change between the modes, and the demo controls can't appear for a store that can't reset.

### D41. The Pages workflow copies the static folder and overwrites `config.js`
**Decision:** `.github/workflows/pages.yml` runs on every push to `master` (and by hand through `workflow_dispatch`). *Changed after the first deploys: pushes only trigger it when `src/main/resources/META-INF/resources/**` or the workflow file change, so docs-only commits no longer redeploy.* It copies `src/main/resources/META-INF/resources/` into `_site/`, overwrites `_site/config.js` with `export const TASK_STORE = 'local';`, and deploys with the official `actions/upload-pages-artifact@v3` and `actions/deploy-pages@v4`. Nothing is built. Before the first run, the repository needs one setting: Settings → Pages → Source: "GitHub Actions" (to be covered in the README, ticket 06). The workflow was checked with `actionlint` and its copy step was run locally. It can't run for real until it is pushed.
**Alternatives:** A `gh-pages` branch built by a third-party action; editing `config.js` in place with `sed`.
**Why:** The deployed page is exactly the one Quarkus serves, apart from that one line, so the demo always matches the latest UI (user story 53). All page URLs are relative (`app.js`, `api/tasks`), so it works under the `/<repo>/` path that Pages uses. Writing the whole one-line file is simpler and fails more visibly than a pattern replacement.

### D42. The local store is checked by hand, plus a throwaway script
**Decision:** No automated frontend test was added (spec, D15). Besides the clicks in the browser in both modes, the local store's rules were exercised once with a throwaway Node script against an in-memory storage. That script isn't committed.
**Why:** The spec keeps the HTTP API as the only automated seam. The script was a quick way to catch mistakes while writing the store, not a test suite to maintain.

---

## Implementation: ticket 06, README and final docs (2026-11-19)

### D43. The README starts with the demo link and the two readings of the brief
**Decision:** The README opens with a one-paragraph summary, then the GitHub Pages link (`https://george713.github.io/task-management-demo/`) with the warning that the demo stores Tasks in the browser only. Next comes a short "How I read the brief" section (Priority fixed after creation, fields that aren't allowed are rejected), then the run routes, dev mode, tests, the tech stack, a small API table and the links to this log, the ADRs and the glossary. It also names the one-time Pages setting from D41.
**Alternatives:** A short README that only links to the spec and this log.
**Why:** A reviewer should know within a minute what this is and how to try it. The two readings of the brief change what the reviewer sees when they try the API, so they belong up front, not only deep in this log.

### D44. The README contains only commands that were run from a fresh clone
**Decision:** Every command in the README was run as written from a fresh `git clone` of the branch into a temporary folder: `docker compose up --build`, `docker compose down`, `docker compose down -v`, `docker compose up -d db`, `./mvnw package`, `java -jar target/quarkus-app/quarkus-run.jar`, `./mvnw quarkus:dev` and `./mvnw test`, and `mvnw.cmd` on Windows. The plain-Java route uses `docker compose up -d db` (background) instead of D14's `docker compose up db`, so everything runs in one terminal. The README notes that ports 5432 and 8080 must be free and that `./mvnw` needs Git Bash on Windows (or `mvnw.cmd`).
**Why:** The README is what a reviewer actually runs. A command that has only been reasoned about, not run, is the most likely thing to fail on someone else's machine.

### D45. Earlier decisions are corrected with a note, not rewritten
**Decision:** Where the code ended up differing from an earlier decision, the decision keeps its original text and gets an italic note saying what changed (D11's transactions, D14's `-d`, D15's test count, D17's actual slices, and earlier D27). A decision that is no longer true at all would be superseded by a new numbered entry instead.
**Why:** This log is the story of how the project came together (D7). Quietly rewriting old entries would hide that the plan changed, and the changes are part of the reasoning.

---

## Code review (2026-11-19)

The finished integration branch was reviewed on two axes, the repo's standards and the spec with this log. The findings were fixed in one follow-up branch. These are the decisions it made or recorded.

### D46. Names are trimmed exactly like JavaScript's `trim()`
**Decision:** `Task.trimName` removes the characters that JavaScript's `String.prototype.trim()` removes: tab, line feed, vertical tab, form feed, carriage return, the byte order mark (U+FEFF), the line and paragraph separators, and every Unicode space separator, including the no-break spaces U+00A0, U+2007 and U+202F. The request objects and the entity both use it.
**Alternatives:** Keep `String.strip()` on the server and copy its rules into the demo by hand.
**Why:** `String.strip()` keeps no-break spaces, so the API accepted a name made only of U+00A0 while the demo rejected it as blank. User story 41 asks the demo to behave like the real app. JavaScript's rule is the one the browser already has built in, so the server copies it, not the other way round. Tests pin down that such a name is rejected and that the characters are trimmed around a real name.

### D47. Request values must have the right JSON type
**Decision:** `StrictJsonTypes` configures Jackson to refuse conversions: a number or boolean is not a `name`, a string or number is not `done`, and a number is not a `priority` (by default Jackson reads `1` as the second constant, NORMAL). Each is a 400: "Invalid value for name", "Invalid value for done", "Invalid priority '1'. Allowed values: LOW, NORMAL, URGENT". The local store uses the same message for a name that is not a string.
**Alternatives:** Leave Jackson's lenient defaults and accept `{"done": "true"}`.
**Why:** The demo already rejected these values, so the API and the demo disagreed (user story 41). Rejecting them also fits D12: the API refuses what it doesn't expect instead of guessing.

### D48. The JSON error mapper names `MismatchedInputException` explicitly
**Decision:** `ErrorMappers.unreadableBody` is registered for `JsonProcessingException` and `MismatchedInputException`. The two subclasses that were also listed (`InvalidFormatException`, `UnrecognizedPropertyException`) were removed.
**Why:** Quarkus picks the mapper registered for the closest exception type, and it ships its own mapper for `MismatchedInputException`. Without naming it, that built-in mapper wins for wrong types, unknown enum values and unknown fields (ten tests failed when it was left out). Its subclasses need no entry of their own, because our entry is already closer than the parent `JsonProcessingException`.

### D49. An empty list shows "No Tasks yet."
**Decision:** When there are no Tasks, the page shows the line "No Tasks yet." where the list would be. It disappears as soon as a Task exists.
**Why:** The spec doesn't describe the empty state. A blank area below the form looks broken on first visit, especially in the demo, where every reviewer starts with an empty list.

### D50. Pressing Enter on a focused name starts a rename
**Decision:** Each name can be reached with Tab, and pressing Enter on it opens the inline rename, the same as clicking it (D31).
**Why:** The spec only says "click to edit". Without a keyboard way in, renaming would be the only action on the page that needs a mouse. Enter is also the key that saves the rename, so the keyboard flow stays simple.

---

## How this was built

1. **Grilling session.** The author worked through every open question with an agent, as a decision tree. The answers are D1–D18 in this file, the ADRs and the glossary.
2. **Spec.** The settled decisions were written into one spec: `.scratch/task-management/spec.md`.
3. **Vertical-slice tickets.** The spec was split into six tickets under `.scratch/task-management/issues/`, each one a working feature end to end (D17), with "blocked by" links between them.
4. **Parallel agent implementers in worktrees.** An orchestrating agent handed each unblocked ticket to its own implementer agent. Each implementer worked in a separate git worktree on its own `ticket/NN-...` branch, so tickets that didn't depend on each other (for example 02 and 04, or 03 and 05) were built at the same time.
5. **Test-first.** Each implementer agreed the seam first (the HTTP API, D15) and then worked red → green, one test and one small piece of code at a time. Docs-only and frontend work was checked by running it (D42, D44). Decisions made along the way were appended here (D19 onwards).
6. **Integration branch.** Finished tickets were merged into `feature/task-management`. A ticket that started before another one landed merged the integration branch back in and re-ran the tests before handing over (visible as merge commits in the history). The orchestrator, not the implementers, closed the tickets.
7. **Code review at the end.** Once all tickets were merged, the whole integration branch was reviewed on two axes: against the repo's standards, and against the spec and these decisions. All findings were fixed in one follow-up branch, test-first where behaviour changed (D46–D50), and merged into the integration branch before it went to `master`. Refactoring belongs to this review, not to the red → green loop.
