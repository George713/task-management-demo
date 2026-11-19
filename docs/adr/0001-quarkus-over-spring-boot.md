# Quarkus 3 over Spring Boot 3

The brief allows either framework. We chose Quarkus 3 (Java 17, Maven). Dev Services start a throwaway Postgres container automatically in dev and test mode, so the project needs no local database setup. Quarkus also starts quickly and builds small container images, and it fits the reviewer's ecosystem. We accepted the trade-off that Spring Boot has more community examples, and therefore more AI-agent familiarity, which matters because the author is new to Java and builds the project agentically.
