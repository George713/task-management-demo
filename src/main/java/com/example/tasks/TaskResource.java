package com.example.tasks;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

/** HTTP layer for Tasks: JSON in, validation, one transaction per change, JSON out. No business rules. */
@Path("/api/tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {

    /**
     * The fields a client may send when creating a Task. Any other field (id, done, created) is
     * refused rather than silently ignored (D12, see
     * quarkus.jackson.fail-on-unknown-properties). The name is trimmed before it is validated.
     */
    public record CreateTaskRequest(
            @NotBlank(message = "Name must not be blank")
            @Size(max = 255, message = "Name must be at most 255 characters")
            String name,
            Priority priority) {

        public CreateTaskRequest {
            name = name == null ? null : name.strip();
        }
    }

    /** The JSON shape of a Task. */
    public record TaskResponse(Long id, String name, boolean done, Instant created, Priority priority) {
        static TaskResponse from(Task task) {
            return new TaskResponse(task.getId(), task.getName(), task.isDone(), task.getCreated(), task.getPriority());
        }
    }

    @POST
    @Transactional
    public Response create(@NotNull(message = "Request body is required") @Valid CreateTaskRequest request) {
        Task task = Task.create(request.name(), request.priority());
        task.persist();
        return Response.status(Response.Status.CREATED).entity(TaskResponse.from(task)).build();
    }
}
