package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CreateTaskTest {

    @Test
    void createWithNameOnlyTrimsNameAndAppliesDefaults() {
        Instant before = Instant.now();

        String created = given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"  Buy milk  \"}")
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("name", equalTo("Buy milk"))
                .body("done", equalTo(false))
                .body("priority", equalTo("NORMAL"))
                .extract().path("created");

        Instant createdAt = Instant.parse(created);
        Duration tolerance = Duration.ofSeconds(5);
        assertTrue(!createdAt.isBefore(before.minus(tolerance)) && !createdAt.isAfter(Instant.now().plus(tolerance)),
                "created should be the current instant, was " + createdAt);
    }

    @Test
    void createWithExplicitPriority() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Fix the roof\", \"priority\": \"URGENT\"}")
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(201)
                .body("name", equalTo("Fix the roof"))
                .body("priority", equalTo("URGENT"))
                .body("done", equalTo(false));
    }

    @Test
    void blankNameIsRejected() {
        rejected("{\"name\": \"   \"}", "name");
    }

    @Test
    void nameOverLimitAfterTrimmingIsRejected() {
        rejected("{\"name\": \"" + "a".repeat(256) + "\"}", "255");
    }

    @Test
    void nameAtLimitAfterTrimmingIsAccepted() {
        String name = "b".repeat(255);
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"  " + name + "  \"}")
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(201)
                .body("name", equalTo(name));
    }

    /** Posts the body and expects a 400 whose readable message mentions the given text. */
    private static void rejected(String body, String messageMentions) {
        given()
                .contentType(ContentType.JSON)
                .body(body)
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", containsStringIgnoringCase(messageMentions));
    }
}
