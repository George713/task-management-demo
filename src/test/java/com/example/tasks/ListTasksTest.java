package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ListTasksTest {

    // Other tests share the database, so this test only looks at the Tasks it created itself.
    private final String marker = UUID.randomUUID().toString();

    @Test
    void listsUrgentBeforeNormalBeforeLowAndNewestFirstWithinAPriority() {
        create("low", "LOW");
        create("older urgent", "URGENT");
        create("normal", "NORMAL");
        create("newer urgent", "URGENT");

        List<String> names = given()
        .when()
                .get("/api/tasks")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().jsonPath().getList("name", String.class);

        List<String> mine = names.stream()
                .filter(name -> name.startsWith(marker))
                .map(name -> name.substring(marker.length() + 1))
                .toList();
        assertEquals(List.of("newer urgent", "older urgent", "normal", "low"), mine);
    }

    @Test
    void listedTaskMatchesTheCreatedTaskExactly() {
        Map<String, Object> created = given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + marker + " exact\"}")
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(201)
                .extract().jsonPath().getMap("$");

        List<Map<String, Object>> listed = given()
        .when()
                .get("/api/tasks")
        .then()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        assertEquals(List.of(created), listed.stream().filter(task -> task.get("id").equals(created.get("id"))).toList());
    }

    private void create(String name, String priority) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + marker + " " + name + "\", \"priority\": \"" + priority + "\"}")
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(201);
    }
}
