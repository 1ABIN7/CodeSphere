package com.CodeSphere.backend.dto.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Response Dto for a test case.
 * For candidates, only sample test cases expose input/output data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCaseResponse {

    private Long id;
    private String inputData;
    private String expectedOutput;
    private Boolean isSample;
    private String explanation;
    private Integer orderIndex;
    private Integer scoreWeight;
}
