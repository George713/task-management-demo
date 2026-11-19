package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void markDoneAndUnmarkDoneKeepTheOtherFields() {
        JsonPath task = create("Water plants", "LOW");
        long id = task.getLong("id");

        patch(id, "{\"done\": true}")
                .statusCode(200)
                .body("done", equalTo(true))
                .body("name", equalTo("Water plants"))
                .body("priority", equalTo("LOW"))
                .body("created", equalTo(task.getString("created")));

        patch(id, "{\"done\": false}")
                .statusCode(200)
                .body("done", equalTo(false))
                .body("name", equalTo("Water plants"));
    }

    @Test
    void renameAndMarkDoneInOnePatchArePersisted() {
        long id = create("Call mum", "NORMAL").getLong("id");

        patch(id, "{\"name\": \"Call dad\", \"done\": true}")
                .statusCode(200)
                .body("name", equalTo("Call dad"))
                .body("done", equalTo(true));

        Map<String, Object> listed = listed(id);
        assertEquals("Call dad", listed.get("name"));
        assertEquals(true, listed.get("done"));
    }

    @Test
    void emptyPatchLeavesTheTaskUnchanged() {
        JsonPath task = create("Read a book", "LOW");
        long id = task.getLong("id");

        Map<String, Object> afterEmptyObject = patch(id, "{}").statusCode(200).extract().jsonPath().getMap("$");
        assertEquals(task.getMap("$"), afterEmptyObject);

        Map<String, Object> afterNoBody = given()
                .contentType(ContentType.JSON)
        .when()
                .patch("/api/tasks/" + id)
        .then()
                .statusCode(200)
                .extract().jsonPath().getMap("$");
        assertEquals(task.getMap("$"), afterNoBody);
        assertEquals(task.getMap("$"), listed(id));
    }

    @Test
    void changingAnUnknownTaskIsNotFound() {
        patch(Long.MAX_VALUE, "{\"done\": true}")
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("message", containsStringIgnoringCase("not found"));
    }

    /** The Task with this id as GET /api/tasks returns it. */
    private static Map<String, Object> listed(long id) {
        List<Map<String, Object>> tasks = given()
        .when()
                .get("/api/tasks")
        .then()
                .statusCode(200)
                .extract().jsonPath().getList("$");
        return tasks.stream()
                .filter(task -> ((Number) task.get("id")).longValue() == id)
                .findFirst().orElseThrow();
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
