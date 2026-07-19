package com.CodeSphere.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standardized error response for all API errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
