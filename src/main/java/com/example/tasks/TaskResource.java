package com.example.tasks;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;

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

    /**
     * The fields a client may send when changing a Task. A field left out (or null) stays
     * unchanged. Priority and Created can't be changed (D4, D5), so sending them is refused like
     * any other unknown field. A name that is sent is trimmed, then must not be blank.
     */
    public record ChangeTaskRequest(
            @Size(min = 1, message = "Name must not be blank")
            @Size(max = 255, message = "Name must be at most 255 characters")
            String name,
            Boolean done) {

        public ChangeTaskRequest {
            name = name == null ? null : name.strip();
        }
    }

    /** The JSON shape of a Task. */
    public record TaskResponse(Long id, String name, boolean done, Instant created, Priority priority) {
        static TaskResponse from(Task task) {
            return new TaskResponse(task.getId(), task.getName(), task.isDone(), task.getCreated(), task.getPriority());
        }
    }

    @GET
    public List<TaskResponse> list() {
        return Task.listInDisplayOrder().stream().map(TaskResponse::from).toList();
    }

    @POST
    @Transactional
    public Response create(@NotNull(message = "Request body is required") @Valid CreateTaskRequest request) {
        Task task = Task.create(request.name(), request.priority());
        task.persist();
        return Response.status(Response.Status.CREATED).entity(TaskResponse.from(task)).build();
    }

    @PATCH
    @Path("{id}")
    @Transactional
    public TaskResponse change(@PathParam("id") long id, @Valid ChangeTaskRequest request) {
        Task task = Task.findById(id);
        if (task == null) {
            throw notFound(id);
        }
        if (request != null && request.name() != null) {
            task.rename(request.name());
        }
        if (request != null && request.done() != null) {
            task.markDone(request.done());
        }
        return TaskResponse.from(task);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        Task.deleteById(id);
    }

    /** A 404 with the same {@code {"message": "..."}} body as every 400 (D22). */
    private static NotFoundException notFound(long id) {
        return new NotFoundException(Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMappers.ErrorResponse("Task " + id + " was not found. It may have been deleted."))
                .build());
    }
}
