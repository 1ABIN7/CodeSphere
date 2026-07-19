package com.CodeSphere.backend.dto.problem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Request Dto for creating or updating a coding problem.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String inputFormat;

    private String outputFormat;

    private String constraints;

    @NotBlank(message = "Difficulty is required")
    private String difficulty; // EASY, MEDIUM, HARD

    private Integer timeLimit; // milliseconds, defaults to 1000

    private Integer memoryLimit; // KB, defaults to 262144

    private List<String> tags;

    private List<String> hints;

    private String editorial;

    private String editorialCode;

    private Map<String, String> starterCode; // language -> code template

    private List<String> companyTags;

    @Valid
    private List<TestCaseRequest> testCases;
}
