package com.CodeSphere.backend.dto.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for submitting code against a problem.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionRequest {

    @NotNull(message = "Problem ID is required")
    private Long problemId;

    @NotBlank(message = "Language is required")
    private String language; // java, python, cpp, c, javascript

    @NotBlank(message = "Code is required")
    private String code;
}
