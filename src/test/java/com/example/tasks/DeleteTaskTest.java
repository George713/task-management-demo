package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DeleteTaskTest {

    @Test
    void deletedTaskIsGoneFromTheList() {
        long id = create("Throw out old shoes");
        assertTrue(listedIds().contains(id));

        delete(id)
                .statusCode(204)
                .body(emptyString());

        assertFalse(listedIds().contains(id));
    }

    @Test
    void deletingAnUnknownTaskIsNotFound() {
        delete(Long.MAX_VALUE)
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("message", containsStringIgnoringCase("not found"));
    }

    @Test
    void deletingTheSameTaskTwiceIsNotFoundTheSecondTime() {
        long id = create("Cancel gym membership");

        delete(id).statusCode(204);
        delete(id)
                .statusCode(404)
                .body("message", containsStringIgnoringCase("not found"));
    }

    private static List<Long> listedIds() {
        return given()
        .when()
                .get("/api/tasks")
        .then()
                .statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
    }

    private static long create(String name) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + name + "\"}")
        .when()
                .post("/api/tasks")
        .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static io.restassured.response.ValidatableResponse delete(long id) {
        return given()
        .when()
                .delete("/api/tasks/" + id)
        .then();
    }
}
