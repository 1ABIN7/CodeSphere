package com.CodeSphere.backend.dto.submission;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Full submission result with per-test-case verdicts and AI analysis.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionResponse {

    private Long id;
    private Long problemId;
    private String problemTitle;
    private String language;
    private String code;
    private String status;
    private Integer score;
    private Integer execTime;
    private Integer execMemory;
    private String errorMessage;

    // Test case summary
    private Integer testCasesPassed;
    private Integer totalTestCases;

    // Per-test-case results
    private List<TestCaseResultResponse> testCaseResults;

    // AI analysis
    private Map<String, Object> aiFeedback;
    private Map<String, Object> complexityAnalysis;

    private OffsetDateTime createdAt;

    /**
     * Individual test case execution result.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TestCaseResultResponse {
        private Long testCaseId;
        private String status;
        private String actualOutput;
        private String expectedOutput; // only for sample test cases
        private String input;          // only for sample test cases
        private Integer execTime;
        private Integer execMemory;
        private String errorOutput;
        private Boolean isSample;
    }
}
