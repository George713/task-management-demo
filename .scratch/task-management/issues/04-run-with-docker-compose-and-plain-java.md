# 04: Run with Docker Compose and with plain Java

**What to build:** A reviewer can start the full application (app + Postgres) with one command that needs only Docker, and can also build with Maven and run the JAR directly with Java against a Postgres container, as the brief requires. Dev mode and tests keep using Dev Services. See `spec.md` (Running and delivery) and D14 in `DECISIONS.md`.

**Blocked by:** 01 (Create and list Tasks)

**Status:** ready-for-agent

- [ ] A multi-stage Dockerfile: the build stage packages the JAR with the Maven wrapper, the runtime stage runs it on a slim Java 17 runtime. Building the image needs no local Java or Maven.
- [ ] A Docker Compose file defines `db` (Postgres with a healthcheck) and `app` (starts only once `db` is healthy, configured through environment variables, exposed on port 8080).
- [ ] `docker compose up --build` from a clean checkout brings up the app at `localhost:8080`, Flyway migrates the schema, and Tasks can be created and listed. Tasks survive restarting the containers (named volume for Postgres data).
- [ ] Plain-Java route works: `docker compose up db`, then `./mvnw package`, then running the JAR with `java -jar`. The prod-profile default database settings point to the Compose Postgres on localhost.
- [ ] Database connection settings apply only to the prod profile. `./mvnw quarkus:dev` and `./mvnw test` still get Postgres from Dev Services.
- [ ] A `.dockerignore` keeps build output and VCS data out of the build context.
- [ ] Both routes are verified by actually running them. Exact commands are noted in this ticket's Comments for the README (ticket 06).
- [ ] Any new decision is appended to `DECISIONS.md`.

## Comments

### Verified commands (2026-11-19, Windows 11, Docker Desktop 29, Git Bash)

One-command route (needs only Docker):

```sh
docker compose up --build            # add -d to run in the background
# open http://localhost:8080
docker compose down                  # stop; Tasks stay in the task-management_db-data volume
docker compose down -v               # stop and delete the data
```

Verified: the app logs "Profile prod activated" and Flyway "Successfully applied 1 migration"; `POST /api/tasks` returned 201-style JSON with an id, `GET /api/tasks` listed it, `GET /` returned 200. After `docker compose down` and `docker compose up -d`, the Task was still listed.

Plain-Java route (needs Java 17 and Docker):

```sh
docker compose up -d db
./mvnw package                       # runs the tests too (they need Docker for Dev Services)
java -jar target/quarkus-app/quarkus-run.jar
# open http://localhost:8080
```

Verified: Flyway connected to `jdbc:postgresql://localhost:5432/tasks` (the Compose db) with the prod defaults, no environment variables set; creating and listing Tasks worked and showed Tasks created earlier through the Compose route.

Dev mode and tests (Dev Services):

```sh
./mvnw quarkus:dev                   # starts its own Postgres container
./mvnw test
```

Verified: both started a Dev Services `postgres:16-alpine` container even while the Compose db was running on port 5432. Dev mode listed no Tasks (separate database). `./mvnw test`: 12 tests, 0 failures, before merging ticket 02.

Notes for the README (ticket 06): on Windows, `./mvnw` needs Git Bash, or use `mvnw.cmd`. Port 5432 must be free for the Compose db.
