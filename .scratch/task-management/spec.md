# Spec: Task Management Application

Status: resolved

Vocabulary follows `GLOSSARY.md` (Task, Priority, Done, Created). Decisions referenced as D1–D18 live in `DECISIONS.md`; architectural decisions in `docs/adr/0001`–`0003`.

## Problem Statement

A person needs a simple way to keep track of Tasks in the browser: write them down with a Priority, mark them Done when the work is finished (or un-mark them), fix a Task's name, and delete Tasks they no longer need. The Tasks must survive restarts, so they are persisted in Postgres.

At the same time, this is an interview submission for Fairsource. The reviewer must be able to (a) try the application with as little effort as possible, (b) build and run the real thing with Maven, directly with Java or as a Docker container, and (c) understand the reasoning behind every decision. The author is new to Java and builds the project with agents, so the structure must stay small and explainable.

## Solution

A Quarkus 3 web application (Java 17, Maven, JAR packaging) that persists Tasks in Postgres and serves a single, minimal English web page. On that page the user creates Tasks, renames them, marks them Done or not Done, and deletes them. The page talks to the backend through a small JSON REST API.

The same page can also run without any backend. In that "demo mode" Tasks are kept in the browser's local storage. This mode is published on GitHub Pages so reviewers can click through the UI immediately. The real application starts with one command (`docker compose up --build`) that needs only Docker. It can also be built with the Maven wrapper and run with plain Java against a Postgres container.

## User Stories

### Managing Tasks (end user)

1. As a user, I want to see all my Tasks on one page, so that I have an overview of what I need to do.
2. As a user, I want to create a Task by typing a name, so that I can capture work quickly.
3. As a user, I want to choose a Priority (LOW, NORMAL, URGENT) when creating a Task, so that I can signal how pressing it is.
4. As a user, I want the Priority to default to NORMAL, so that I don't have to choose one for ordinary Tasks.
5. As a user, I want a newly created Task to start as not Done, so that new work shows up as open.
6. As a user, I want the moment a Task was Created to be recorded automatically, so that I don't have to enter it and it can't be faked.
7. As a user, I want leading and trailing whitespace removed from a Task's name, so that accidental spaces don't create messy entries.
8. As a user, I want to be told when I try to create a Task with an empty or blank name, so that I don't create meaningless Tasks.
9. As a user, I want to be told when a Task name is longer than 255 characters, so that I understand why it was rejected.
10. As a user, I want to mark a Task as Done with a single click, so that I can track my progress.
11. As a user, I want to un-mark a Done Task, so that I can correct a mistake or reopen work.
12. As a user, I want to rename a Task by clicking its name and editing it in place, so that I can fix typos or clarify it.
13. As a user, I want renaming to follow the same name rules as creation (not blank, trimmed, at most 255 characters), so that Task names are consistently valid.
14. As a user, I want to cancel a rename without saving, so that I can back out of an accidental edit.
15. As a user, I want to delete a Task, so that my list contains only Tasks I care about.
16. As a user, I want each Task's Priority to be clearly visible (e.g. as a badge), so that I can spot URGENT Tasks at a glance.
17. As a user, I want open Tasks listed before Done Tasks, so that the work still to do is on top.
18. As a user, I want Tasks within each group ordered URGENT, then NORMAL, then LOW, so that the most pressing work comes first.
19. As a user, I want Tasks with equal Done state and Priority ordered newest first, so that recent additions are easy to find.
20. As a user, I want my Tasks to still be there after I reload the page or the server restarts, so that I can rely on the application.
21. As a user, I want error messages shown inline on the page, so that I understand what went wrong without opening developer tools.
22. As a user, I want the list to reflect the change right after I create, change or delete a Task, so that I trust what I see.
23. As a user, I want to be told if a Task I'm acting on no longer exists (e.g. deleted in another tab), so that I'm not confused by a silent failure.

### Priority is fixed at creation

24. As a user, I want a Task's Priority to be fixed once it is created, so that the application matches the brief, which only allows changing the name and Done.
25. As an API client, I want an attempt to change a Task's Priority to be rejected with a clear error, so that I learn the rule instead of having my input silently ignored.
26. As an API client, I want an attempt to set or change Created to be rejected with a clear error, so that the server stays the only source of that timestamp.

### API (programmatic access)

27. As an API client, I want to list all Tasks in the agreed order with one request, so that any client can render the list without sorting.
28. As an API client, I want to create a Task by sending its name and an optional Priority, and receive the created Task (with id, Done = false and Created) and status 201, so that I know exactly what was stored.
29. As an API client, I want to change only the fields I send (name and/or Done), so that I don't have to resend the whole Task.
30. As an API client, I want a 404 response when I change or delete a Task id that doesn't exist, so that I can handle it correctly.
31. As an API client, I want a 400 response with a readable message for invalid input, so that I can fix my request.
32. As an API client, I want a 204 response on a successful delete, so that I know it worked without a response body.
33. As an API client, I want Priority values to be exactly LOW, NORMAL or URGENT, and anything else rejected, so that the data stays clean.

### Data integrity

34. As the system owner, I want the database itself to reject rows with a missing name, an over-long name, an unknown Priority or a missing Created, so that data stays valid even if someone bypasses the application.
35. As the system owner, I want the application to refuse to start if its view of the Task table doesn't match the real database table, so that schema drift is caught immediately.
36. As the system owner, I want every schema change to be a versioned, reviewable migration applied automatically at startup, so that every environment has exactly the same schema.

### Reviewer experience

37. As a Fairsource reviewer, I want to open a link and try the UI immediately without installing anything, so that I get a first impression in seconds.
38. As a Fairsource reviewer, I want the demo to clearly say that its Tasks are stored only in my browser and not in Postgres, so that I'm not misled about what I'm looking at.
39. As a Fairsource reviewer, I want a reset button in demo mode, so that I can start from a clean slate.
40. As a Fairsource reviewer, I want demo-mode Tasks to survive a page reload, so that the demo behaves like the real app.
41. As a Fairsource reviewer, I want demo mode to follow the same rules as the real app (defaults, name validation, fixed Priority, ordering), so that the demo is a faithful preview.
42. As a Fairsource reviewer, I want to start the full application (app + Postgres) with a single Docker Compose command that doesn't require Java on my machine, so that running the real thing takes no effort.
43. As a Fairsource reviewer, I want documented steps to build with Maven and run the JAR directly with Java against a Postgres container, so that I can verify the brief's "run directly with Java" requirement.
44. As a Fairsource reviewer, I want the Maven wrapper included, so that I don't need a matching Maven installed.
45. As a Fairsource reviewer, I want a README explaining how to run, build, test and open the demo, so that I don't have to guess.
46. As a Fairsource reviewer, I want a decision log and ADRs explaining why things were built this way, so that the technical interview can focus on reasoning.
47. As a Fairsource reviewer, I want the project generated from code.quarkus.io with Quarkus 3 (non-snapshot), Java 17, Maven and JAR packaging, so that it meets the brief's constraints.
48. As a Fairsource reviewer, I want the tests to run with one Maven command, so that I can check the behaviour is verified.

### Developer experience

49. As a developer, I want to start the app in dev mode with one command and have Postgres started for me automatically, so that I need no manual database setup.
50. As a developer, I want the tests to run against a real, automatically started Postgres, so that they exercise the same database as production.
51. As a developer, I want the UI to reach Tasks only through one task-store interface, so that the backend can be swapped between the REST API and browser storage without touching the UI.
52. As a developer, I want the storage mode chosen by one explicit configuration value, so that it's obvious which backend a deployment uses.
53. As a developer, I want the GitHub Pages deployment to be automated from the repository, so that the demo always matches the latest UI.

## Implementation Decisions

### Platform and build
- Quarkus 3 (latest stable, non-snapshot), Java 17, Maven, JAR packaging, generated from code.quarkus.io. Maven wrapper committed. (D1, ADR-0001)
- Extensions: REST with JSON (Jackson), Hibernate ORM with Panache, JDBC Postgres driver, Flyway, Hibernate Validator. Nothing beyond what the use cases need.

### Backend structure: two classes (D11)
- **Task entity**: one Task object is one row. It extends Panache's base entity (generated numeric id plus built-in persist/find/delete/list).
  - Fields: name, done, created (Instant), priority (Priority enum).
  - New Tasks are made only through a factory, `create(name, priority)`. It trims the name, defaults Priority to NORMAL when absent, sets Created to the current instant and Done to false.
  - Changes only through `rename(name)` (trims) and `markDone(boolean)`.
  - No way to change Priority or Created after creation. That is the code-level form of D4 and D5.
- **Priority enum**: LOW, NORMAL, URGENT. Persisted as its name (text), never its ordinal position.
- **TaskResource**: the HTTP layer for every Task endpoint. It turns JSON into request objects that contain only the allowed fields, validates them (not blank, max 255), runs each changing request in a transaction, and turns domain objects back into JSON responses. It contains no business rules beyond input validation.
- No service layer and no separate repository class. A service layer is the planned next step only if multi-step use cases appear.
- Sorting (open before Done, then URGENT → NORMAL → LOW, then newest Created first) happens in the database query. Because Priority is stored as text, the order must be explicit (a CASE expression or equivalent), not alphabetical.

### API contract (D12)
- `GET /api/tasks` returns 200 with a JSON array of Tasks in the agreed order.
- `POST /api/tasks` takes `{name, priority?}` and returns 201 with the created Task.
- `PATCH /api/tasks/{id}` takes `{name?, done?}` and returns 200 with the updated Task. Fields that are left out stay unchanged.
- `DELETE /api/tasks/{id}` returns 204.
- Task JSON shape: `{id, name, done, created, priority}`. `created` is an ISO-8601 UTC timestamp.
- Errors: 400 for invalid input (blank or over-long name, unknown Priority, malformed JSON) with a readable message. 404 for an unknown id.
- A `priority` or `created` field in a PATCH body, or `created`/`done`/`id` in a POST body, is rejected with 400 rather than silently ignored (unknown properties are refused for request objects).

### Schema (D6)
- Managed by Flyway. One initial versioned migration creates the `task` table. Migrations run at startup.
- Columns: generated id (primary key); `name` (VARCHAR(255), NOT NULL); `done` (BOOLEAN, NOT NULL, default false); `created` (TIMESTAMPTZ, NOT NULL); `priority` (VARCHAR, NOT NULL, CHECK in LOW/NORMAL/URGENT).
- Hibernate's schema generation is off. Hibernate validates the entity mapping against the real schema at startup and fails fast on mismatch.
- Rules are enforced at three layers on purpose (defence in depth): Postgres holds the hard data rules, the entity holds the behaviour rules, request validation produces friendly 400s.

### Frontend (D2, D8, D9, D16; ADR-0002, ADR-0003)
- One static English HTML page with plain JavaScript (ES modules, no framework, no npm build), served by Quarkus as a static resource.
- Styling from one classless stylesheet (Pico.css, pinned version from an allowed CDN).
- Layout: a create form (name input, Priority dropdown defaulting to NORMAL, Add button) above the Task list. Each row: Done checkbox, name (click to edit in place; Enter saves, Escape cancels), Priority badge, delete button. Errors are shown inline.
- **Task-store interface** with operations list, create(name, priority), update(id, {name?, done?}) and remove(id). It returns Tasks in the API's JSON shape and reports errors as readable messages. The UI uses only this interface.
  - **ApiTaskStore** calls the REST API.
  - **LocalTaskStore** keeps Tasks in `localStorage` and mirrors the server rules: trimming, not blank, max 255, NORMAL default, Done false, Created set at creation, Priority fixed, same sort order, "not found" for unknown ids. The duplication is accepted (ADR-0003).
- A one-line config script selects the store: `api` in the Quarkus app, `local` in the Pages deployment. Demo mode shows a persistent banner ("Demo mode: tasks are stored in your browser only, not in Postgres") and a reset button that clears the stored Tasks.

### Running and delivery (D10, D14)
- Multi-stage Dockerfile: the build stage runs the Maven wrapper to package the JAR, the runtime stage runs it on a slim Java 17 runtime. A reviewer needs no local Java for this route.
- Docker Compose defines `db` (Postgres, with a healthcheck) and `app` (waits for a healthy db, configured via environment variables, exposed on port 8080). `docker compose up --build` is the one-command route.
- Plain-Java route: start only `db` with Compose, package with the Maven wrapper, run the JAR. Its default database settings point to the Compose Postgres on localhost.
- Dev mode and tests rely on Quarkus Dev Services to start Postgres automatically. The prod connection settings apply only to the prod profile, so Dev Services stay active in dev and test.
- A GitHub Actions workflow publishes the static frontend to GitHub Pages with the config switched to `local`.
- The repository is public. The README covers: demo link, one-command run, plain-Java run, dev mode, tests, and pointers to DECISIONS.md, the ADRs and GLOSSARY.md.

## Testing Decisions

- **Single seam: the HTTP API.** All automated tests drive the application through the REST API with `@QuarkusTest` and RestAssured, against a real Postgres started by Dev Services (Docker required to run tests). This is the highest seam and the one reviewers actually use, and tests written there survive internal refactoring. (D15)
- **What makes a good test here:** it checks externally visible behaviour (status codes, response bodies, list contents and order after a sequence of calls), never internal details such as which methods were called or how the entity is built. Each test sets up its own data through the API and does not depend on other tests' data.
- **Behaviours to cover:**
  - Create with name only (defaults NORMAL, Done false, Created set, name trimmed); create with explicit Priority.
  - Create rejected: blank name, over-long name, unknown Priority, client-supplied Created/Done/id.
  - List order across Done state, Priority and Created.
  - Rename (trimmed); rename rejected when blank or over-long; mark Done and un-mark Done; PATCH with both fields; PATCH with no fields leaves the Task unchanged.
  - PATCH rejected when it tries to change Priority or Created.
  - Delete returns 204 and the Task is gone from the list; change or delete of an unknown id returns 404.
- **Not tested automatically:** the frontend, including LocalTaskStore (verified manually in the browser, in both modes). No mock-based unit tests.
- **Prior art:** none. The repository has no code yet. The first API test written in the scaffold/create slice is the reference pattern for the later slices.

## Out of Scope

- Changing a Task's Priority after creation (deliberate, per the brief).
- Users, authentication, authorisation, multi-user or per-user Task lists.
- Due dates, descriptions, tags, search, filtering, pagination, bulk actions, undo.
- Real-time sync between browser tabs or clients.
- Internationalisation (UI is English only).
- Native-image builds, Kubernetes manifests, cloud deployment of the backend.
- Automated frontend or end-to-end browser tests.
- Syncing or migrating demo-mode (browser) Tasks into the real database.

## Further Notes

- Implementation proceeds in vertical slices tracked as issues under `.scratch/task-management/issues/` (D17): scaffold → create & list → change → delete → browser demo + Pages → README. Each slice is built test-first by agents. A review against this spec and `DECISIONS.md` happens at the end (D18).
- Any new decision made during implementation is appended to `DECISIONS.md`. If it is hard to reverse, surprising and the result of a real trade-off, it also gets an ADR.
- The brief is in German. "Task ändern (name, bzw. done)" is the source for the fixed-Priority rule.
