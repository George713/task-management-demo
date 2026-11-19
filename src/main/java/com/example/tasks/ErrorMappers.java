package com.example.tasks;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.stream.Collectors;

/** Turns rejected requests into a 400 with one readable message: {@code {"message": "..."}}. */
public class ErrorMappers {

    /** The JSON shape of every error response. */
    public record ErrorResponse(String message) {
    }

    static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(message))
                .build();
    }

    @ServerExceptionMapper
    public Response invalidInput(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }
}
