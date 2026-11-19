package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.example.tasks.TaskApi.post;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CreateTaskTest {

    @Test
    void createWithNameOnlyTrimsNameAndAppliesDefaults() {
        Instant before = Instant.now();

        String created = post("{\"name\": \"  Buy milk  \"}")
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
        post("{\"name\": \"Fix the roof\", \"priority\": \"URGENT\"}")
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
    void missingOrBlankNameGetsExactlyOneMessage() {
        post("{}").statusCode(400).body("message", equalTo("Name must not be blank"));
        post("{\"name\": null}").statusCode(400).body("message", equalTo("Name must not be blank"));
        post("{\"name\": \"  \"}").statusCode(400).body("message", equalTo("Name must not be blank"));
    }

    @Test
    void nameOfOnlyNonBreakingAndOtherUnicodeSpacesIsRejected() {
        // No-break space, ideographic space, byte order mark: the demo's JavaScript trim() removes them too.
        rejected("{\"name\": \"\u00a0\u3000\ufeff\"}", "blank");
    }

    @Test
    void nonBreakingAndOtherUnicodeSpacesAroundTheNameAreTrimmed() {
        post("{\"name\": \"\u00a0\u202f Buy bread\u2007\u3000\"}")
                .statusCode(201)
                .body("name", equalTo("Buy bread"));
    }

    @Test
    void nameOverLimitAfterTrimmingIsRejected() {
        rejected("{\"name\": \"" + "a".repeat(256) + "\"}", "255");
    }

    @Test
    void nameAtLimitAfterTrimmingIsAccepted() {
        String name = "b".repeat(255);
        post("{\"name\": \"  " + name + "  \"}")
                .statusCode(201)
                .body("name", equalTo(name));
    }

    @Test
    void unknownPriorityIsRejected() {
        rejected("{\"name\": \"Water plants\", \"priority\": \"HIGH\"}", "priority");
    }

    @Test
    void nameThatIsNotAStringIsRejected() {
        rejected("{\"name\": 123}", "Invalid value for name");
        rejected("{\"name\": true}", "name");
    }

    @Test
    void priorityThatIsNotAStringIsRejected() {
        // Jackson would otherwise read a number as the enum's position (1 = NORMAL).
        rejected("{\"name\": \"Water plants\", \"priority\": 1}", "Invalid priority '1'");
        rejected("{\"name\": \"Water plants\", \"priority\": true}", "priority");
    }

    @Test
    void malformedJsonIsRejected() {
        rejected("{\"name\": \"Water plants\"", "malformed");
    }

    @Test
    void clientSuppliedCreatedIsRejected() {
        rejected("{\"name\": \"Water plants\", \"created\": \"2020-01-01T00:00:00Z\"}", "created");
    }

    @Test
    void clientSuppliedDoneIsRejected() {
        rejected("{\"name\": \"Water plants\", \"done\": true}", "done");
    }

    @Test
    void clientSuppliedIdIsRejected() {
        rejected("{\"name\": \"Water plants\", \"id\": 42}", "id");
    }

    /** Posts the body and expects a 400 whose readable message mentions the given text. */
    private static void rejected(String body, String messageMentions) {
        post(body)
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", containsStringIgnoringCase(messageMentions));
    }
}
