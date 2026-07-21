package com.CodeSphere.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HtmlSanitizerValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SanitizedHtml {
    String message() default "Invalid or unsafe HTML content detected";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}