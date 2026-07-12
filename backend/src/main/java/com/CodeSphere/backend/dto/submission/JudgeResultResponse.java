package com.CodeSphere.backend.dto.submission;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Detailed AI analysis result from the judge engine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JudgeResultResponse {

    private String overallVerdict;
    private Integer score;
    private Integer totalScore;

    // Complexity analysis
    private String estimatedTimeComplexity;
    private String estimatedSpaceComplexity;

    // Code quality
    private Integer codeQualityScore;  // out of 100
    private List<String> codeQualityIssues;
    private List<String> optimizationSuggestions;

    // Anti-patterns
    private List<String> antiPatterns;

    // Performance hints based on failed test cases
    private List<String> performanceHints;

    // Similarity analysis
    private Double similarityScore;
    private Boolean potentialPlagiarism;

    // Raw metrics
    private Map<String, Object> rawMetrics;
}
