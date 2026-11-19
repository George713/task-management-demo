package com.example.tasks;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A single piece of work someone wants to remember. One object is one row.
 *
 * <p>New Tasks are made only through {@link #create(String, Priority)}. Priority and
 * Created can never change after creation (D4, D5), so there are no setters for them.
 */
@Entity
public class Task extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false, updatable = false)
    private Instant created;

    // Stored as its name ('URGENT'), never its ordinal, so reordering the enum can't corrupt data.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Priority priority;

    /** For Hibernate only. */
    protected Task() {
    }

    /** Makes a new, not yet persisted Task: trimmed name, Priority defaulting to NORMAL, Created now, not Done. */
    public static Task create(String name, Priority priority) {
        Task task = new Task();
        task.name = name.strip();
        task.priority = priority == null ? Priority.NORMAL : priority;
        // Postgres stores microseconds; truncating here keeps the returned Task identical to the stored one.
        task.created = Instant.now().truncatedTo(ChronoUnit.MICROS);
        task.done = false;
        return task;
    }

    /**
     * All Tasks in display order: open before Done, then URGENT, NORMAL, LOW, then newest Created
     * first (id breaks exact ties). Priority is stored as text, so its order is spelled out
     * explicitly instead of sorting alphabetically.
     */
    public static List<Task> listInDisplayOrder() {
        return list("""
                order by done,
                    case priority
                        when com.example.tasks.Priority.URGENT then 0
                        when com.example.tasks.Priority.NORMAL then 1
                        else 2
                    end,
                    created desc,
                    id desc""");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDone() {
        return done;
    }

    public Instant getCreated() {
        return created;
    }

    public Priority getPriority() {
        return priority;
    }
}
