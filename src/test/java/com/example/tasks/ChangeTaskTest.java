package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class ChangeTaskTest {

    @Test
    void renameTrimsTheNameAndKeepsTheOtherFields() {
        JsonPath task = create("Buy milk", "URGENT");

        patch(task.getLong("id"), "{\"name\": \"  Buy oat milk  \"}")
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(task.getInt("id")))
                .body("name", equalTo("Buy oat milk"))
                .body("done", equalTo(false))
                .body("priority", equalTo("URGENT"))
                .body("created", equalTo(task.getString("created")));
    }

    @Test
    void changingAnUnknownTaskIsNotFound() {
        patch(Long.MAX_VALUE, "{\"done\": true}")
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("message", containsStringIgnoringCase("not found"));
    }

    private static JsonPath create(String name, String priority) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + name + "\", \"priority\": \"" + priority + "\"}")
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(201)
                .extract().jsonPath();
    }

    private static io.restassured.response.ValidatableResponse patch(long id, String body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
        .when()
                .patch("/api/tasks/" + id)
        .then();
    }
}
