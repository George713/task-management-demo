package com.example.tasks;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/** Calls to the Task HTTP API shared by the tests. Holds no state, so tests stay independent. */
final class TaskApi {

    private TaskApi() {
    }

    /** POST /api/tasks with this raw JSON body. */
    static ValidatableResponse post(String body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
        .when()
                .post("/api/tasks")
        .then();
    }

    /** Creates a Task with the default Priority and returns it as the API sent it. */
    static JsonPath create(String name) {
        return post("{\"name\": \"" + name + "\"}").statusCode(201).extract().jsonPath();
    }

    /** Creates a Task with this Priority and returns it as the API sent it. */
    static JsonPath create(String name, String priority) {
        return post("{\"name\": \"" + name + "\", \"priority\": \"" + priority + "\"}")
                .statusCode(201).extract().jsonPath();
    }

    /** All Tasks as GET /api/tasks returns them, in list order. */
    static List<Map<String, Object>> list() {
        return given()
        .when()
                .get("/api/tasks")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().jsonPath().getList("$");
    }

    /** The ids of all Tasks, in list order. */
    static List<Long> listedIds() {
        return list().stream().map(task -> ((Number) task.get("id")).longValue()).toList();
    }

    /** The Task with this id as GET /api/tasks returns it. */
    static Map<String, Object> listed(long id) {
        return list().stream()
                .filter(task -> ((Number) task.get("id")).longValue() == id)
                .findFirst().orElseThrow();
    }

    /** PATCH /api/tasks/{id} with this raw JSON body. */
    static ValidatableResponse patch(long id, String body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
        .when()
                .patch("/api/tasks/" + id)
        .then();
    }

    /** DELETE /api/tasks/{id}. */
    static ValidatableResponse delete(long id) {
        return given()
        .when()
                .delete("/api/tasks/" + id)
        .then();
    }
}
