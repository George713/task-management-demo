<img src="assets/icon.svg" width="64" height="64" alt="">

# task-management-demo

A small task list app, built as an interview task. You create Tasks with a name and a Priority (LOW, NORMAL, URGENT), mark them Done or not Done, rename them and delete them, all on one simple web page. The brief asks for a Quarkus 3 or Spring Boot 3 app (Java 17, Maven, JAR packaging) that stores Tasks in Postgres and runs either directly with Java or as a Docker container. This repo uses Quarkus 3 with a small JSON REST API and one plain HTML/JavaScript page.

**Try it in your browser:** <https://george713.github.io/task-management-demo/>

The demo is the same page running without a backend. **It stores Tasks in your browser only (`localStorage`), not in Postgres.** A banner says so, and "Reset demo" clears it. Run the app (below) to get the real Postgres-backed version.

## How I read the brief

Where the brief was open, these are the choices that change what you see:

- **Priority is fixed after creation.** The brief says "Task ändern (name, bzw. done)", so only the name and Done can change. Priority is picked once, when the Task is created (default NORMAL).
- **Fields that aren't allowed are rejected, not ignored.** A `PATCH` that sends `priority` or `created`, or a `POST` that sends `id`, `done` or `created`, gets a `400` with a readable message. A client that tries to break a rule should be told so, not have its input silently dropped.
- Created is always set by the server. Names are trimmed and must be 1 to 255 characters. The list shows open Tasks first, then URGENT → NORMAL → LOW, then newest first.

The reasoning for all of this is in [`DECISIONS.md`](DECISIONS.md) (see below).

## Run it

### Prerequisites

The browser demo needs nothing installed. To run the real app you need:

| What | Needed for | Notes |
| --- | --- | --- |
| **Git** | every route | to clone this repository |
| **Docker** with Compose v2, running | every route | Docker Desktop on Windows and macOS, or Docker Engine plus the Compose plugin on Linux. Compose runs Postgres (and the app), and dev mode and the tests start Postgres through Dev Services. |
| **JDK 17** | plain Java, dev mode, tests | a JDK, not just a JRE, with `java` on your `PATH` or `JAVA_HOME` set. Not needed for the Docker Compose route. |
| Free ports **8080** and **5432** | running the app | 8080 for the app, 5432 for the Compose Postgres |

You don't need Maven: the included Maven wrapper (`./mvnw`, or `mvnw.cmd` on Windows) downloads the right version.

### With Docker Compose (needs only Docker)

```sh
docker compose up --build
```

Open <http://localhost:8080>. This builds the JAR inside Docker (no local Java or Maven needed) and starts Postgres and the app.

```sh
docker compose down       # stop; your Tasks stay in the task-management_db-data volume
docker compose down -v    # stop and delete the data
```

### With plain Java (needs Java 17 and Docker for Postgres)

```sh
docker compose up -d db
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Open <http://localhost:8080>. By default the JAR connects to the Compose Postgres on `localhost:5432` (database, user and password are all `tasks`). To use another database, set `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` and `QUARKUS_DATASOURCE_PASSWORD`. `./mvnw package` also runs the tests, so Docker must be running (see [Tests](#tests)). Stop Postgres afterwards with `docker compose down`.

On Windows, run `./mvnw` from Git Bash, or use `mvnw.cmd` in PowerShell or cmd.

## Develop

```sh
./mvnw quarkus:dev
```

Open <http://localhost:8080>. Dev mode reloads code changes live and starts its own throwaway Postgres container through Quarkus Dev Services, so you need Docker but no database setup. It doesn't use the Compose database, so it starts with an empty list.

## Tests

```sh
./mvnw test
```

**Docker must be running:** the tests start a real Postgres (`postgres:16-alpine`, the same image Compose uses) through Dev Services. They are `@QuarkusTest` + RestAssured tests that go through the HTTP API, covering create, list order, rename, Done, delete and the rule violations (blank or too-long names, unknown Priority, fields that aren't allowed, unknown ids). The frontend is checked by hand, in both modes.

## Tech stack

- **Java 17, Quarkus 3.40 (LTS), Maven** (wrapper included), JAR packaging, generated from code.quarkus.io
- **Quarkus REST + Jackson** for the JSON API, **Hibernate Validator** for request validation
- **Hibernate ORM with Panache** and **Postgres 16**; the schema is a **Flyway** migration
- **Frontend:** one `index.html` with plain JavaScript ES modules (no framework, no npm build), styled with classless **Pico.css**
- **Tests:** JUnit 6 (Jupiter), RestAssured, Quarkus Dev Services (Testcontainers)
- **Delivery:** multi-stage `Dockerfile` + `compose.yaml`; a **GitHub Actions** workflow publishes the browser demo to **GitHub Pages**

### API

| Method | Path | Body | Success |
| --- | --- | --- | --- |
| `GET` | `/api/tasks` | | `200`, all Tasks in display order |
| `POST` | `/api/tasks` | `{"name": "...", "priority": "URGENT"}` (priority optional) | `201`, the new Task |
| `PATCH` | `/api/tasks/{id}` | `{"name": "...", "done": true}` (both optional) | `200`, the changed Task |
| `DELETE` | `/api/tasks/{id}` | | `204` |

A Task looks like `{"id": 1, "name": "...", "done": false, "created": "2026-11-19T10:15:30.123456Z", "priority": "NORMAL"}`. Errors are `400` (bad input) or `404` (unknown id), with a body `{"message": "..."}`.

### How the demo works

The page only talks to Tasks through a small task-store interface. `config.js` picks the implementation: `api` (REST API, what the Quarkus app ships) or `local` (`localStorage`). The Pages workflow ([`.github/workflows/pages.yml`](.github/workflows/pages.yml)) copies the static files and overwrites `config.js` with `local`. Nothing else differs. See [ADR-0003](docs/adr/0003-swappable-task-store-for-browser-demo.md).

The workflow runs on every push to `master`. Before its first run, the repository needs one setting: **Settings → Pages → Build and deployment → Source: "GitHub Actions"**.

## Where the reasoning lives

- [`DECISIONS.md`](DECISIONS.md): every decision in order, with the reasoning and the alternatives that were rejected, plus how the project was built
- [`docs/adr/`](docs/adr/): the few decisions that are hard to reverse (Quarkus over Spring Boot, REST API with a plain-JS page, the swappable task store)
- [`GLOSSARY.md`](GLOSSARY.md): the domain words (Task, Priority, Done, Created)
- [`.scratch/task-management/`](.scratch/task-management/): the spec and the tickets the work was split into

This project was built with AI coding agents, directed by me. I settled the requirements and the architecture; agents wrote most of the code, test-first. At the end, the whole branch was reviewed against the repo's standards and against the spec, and the findings were fixed in one follow-up branch. The decision log is the record of who decided what.
