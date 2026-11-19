package com.example.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.Arrays;
import java.util.List;
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

    /**
     * Replaces Quarkus' built-in Jackson error bodies (which expose parser internals) for the
     * request body problems a client can cause: malformed JSON, a value of the wrong type or an
     * unknown enum constant, and fields that are not allowed (request objects refuse unknown fields).
     */
    @ServerExceptionMapper({
            JsonProcessingException.class,
            MismatchedInputException.class,
            InvalidFormatException.class,
            UnrecognizedPropertyException.class})
    public Response unreadableBody(JsonProcessingException e) {
        if (e instanceof UnrecognizedPropertyException unknown) {
            String allowed = unknown.getKnownPropertyIds().stream()
                    .map(Object::toString).sorted().collect(Collectors.joining(", "));
            return badRequest("Field '" + unknown.getPropertyName() + "' is not allowed. Allowed fields: " + allowed);
        }
        if (e instanceof InvalidFormatException invalid && invalid.getTargetType().isEnum()) {
            String allowed = Arrays.stream(invalid.getTargetType().getEnumConstants())
                    .map(Object::toString).collect(Collectors.joining(", "));
            return badRequest("Invalid " + fieldName(invalid) + " '" + invalid.getValue()
                    + "'. Allowed values: " + allowed);
        }
        if (e instanceof MismatchedInputException mismatched && !mismatched.getPath().isEmpty()) {
            return badRequest("Invalid value for " + fieldName(mismatched));
        }
        return badRequest("Malformed JSON in request body");
    }

    private static String fieldName(JsonMappingException e) {
        List<JsonMappingException.Reference> path = e.getPath();
        return path.isEmpty() ? "value" : path.get(path.size() - 1).getFieldName();
    }

    /**
     * Quarkus wraps a syntax error in the JSON body (e.g. a missing closing brace) in a plain
     * 400 WebApplicationException with an empty body. Give it the readable message too; every
     * other WebApplicationException keeps its own response.
     */
    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e) {
        if (e.getCause() instanceof JsonProcessingException json) {
            return unreadableBody(json);
        }
        return e.getResponse();
    }
}
