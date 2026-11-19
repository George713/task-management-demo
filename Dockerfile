# syntax=docker/dockerfile:1
# Multi-stage build (D14): building the image needs only Docker, no local Java or Maven.

# --- Build stage: package the JAR with the Maven wrapper -------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build

# The wrapper needs unzip: without it, it downloads the .tar.gz instead, which
# fails the checksum pinned for the .zip in maven-wrapper.properties.
RUN apt-get update && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

# Wrapper and POM first, so the dependency download is cached as its own layer
# and only re-runs when pom.xml changes. Quarkus resolves a few more artifacts
# while packaging, so the package step below is not run offline.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests are skipped here: they need Docker (Dev Services) and run with ./mvnw test.
RUN ./mvnw -B package -DskipTests

# --- Runtime stage: run the JAR on a slim Java 17 runtime -------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
# Quarkus fast-jar layout: copy the whole quarkus-app directory.
COPY --from=build --chown=app:app /build/target/quarkus-app/ ./
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
