package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static com.example.tasks.TaskApi.create;
import static com.example.tasks.TaskApi.delete;
import static com.example.tasks.TaskApi.listedIds;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DeleteTaskTest {

    @Test
    void deletedTaskIsGoneFromTheList() {
        long id = create("Throw out old shoes").getLong("id");
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
        long id = create("Cancel gym membership").getLong("id");

        delete(id).statusCode(204);
        delete(id)
                .statusCode(404)
                .body("message", containsStringIgnoringCase("not found"));
    }
}
