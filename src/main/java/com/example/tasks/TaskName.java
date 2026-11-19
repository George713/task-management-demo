package com.example.tasks;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A Task name as a client sends it, already trimmed ({@link Task#trimName(String)}): not blank
 * and at most {@value Task#MAX_NAME_LENGTH} characters. Null passes, so a change can leave the
 * name out; add {@code @NotNull} where a name is required. Each rule reports its own message.
 */
@Size(min = 1, message = "Name must not be blank")
@Size(max = Task.MAX_NAME_LENGTH, message = "Name must be at most " + Task.MAX_NAME_LENGTH + " characters")
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
public @interface TaskName {

    String message() default "Invalid name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
