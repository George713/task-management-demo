package com.example.tasks;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.tasks.TaskApi.list;
import static com.example.tasks.TaskApi.patch;
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

        assertEquals(List.of("newer urgent", "older urgent", "normal", "low"), myNames());
    }

    @Test
    void doneTasksAreListedBelowOpenTasks() {
        long doneUrgent = create("done urgent", "URGENT");
        create("open low", "LOW");
        long doneNormal = create("done normal", "NORMAL");
        create("open urgent", "URGENT");

        patch(doneUrgent, "{\"done\": true}").statusCode(200);
        patch(doneNormal, "{\"done\": true}").statusCode(200);

        assertEquals(List.of("open urgent", "open low", "done urgent", "done normal"), myNames());
    }

    @Test
    void listedTaskMatchesTheCreatedTaskExactly() {
        Map<String, Object> created = TaskApi.create(marker + " exact").getMap("$");

        List<Map<String, Object>> listed = list();

        assertEquals(List.of(created), listed.stream().filter(task -> task.get("id").equals(created.get("id"))).toList());
    }

    /** The names of this test's own Tasks, in list order, without the marker. */
    private List<String> myNames() {
        return list().stream()
                .map(task -> (String) task.get("name"))
                .filter(name -> name.startsWith(marker))
                .map(name -> name.substring(marker.length() + 1))
                .toList();
    }

    /** Creates one of this test's own Tasks (its name prefixed with the marker) and returns its id. */
    private long create(String name, String priority) {
        return TaskApi.create(marker + " " + name, priority).getLong("id");
    }
}
