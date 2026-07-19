package com.CodeSphere.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {}) // You can add your HTML validator class logic here later
public @interface SanitizedHtml {
    String message() default "Invalid or unsafe HTML content detected";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}