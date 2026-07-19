package com.CodeSphere.backend.dto.problem;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request Dto for creating or updating a test case.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseRequest {

    @NotBlank(message = "Input data is required")
    private String inputData;

    @NotBlank(message = "Expected output is required")
    private String expectedOutput;

    private Boolean isSample;

    private String explanation;

    private Integer orderIndex;

    private Integer timeLimitOverride;

    private Integer scoreWeight;
}
