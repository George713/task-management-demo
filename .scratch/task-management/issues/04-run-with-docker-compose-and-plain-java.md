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
