package com.CodeSphere.backend.dto.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detailed problem response with all metadata and sample test cases.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemResponse {

    private Long id;
    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private String difficulty;
    private Integer timeLimit;
    private Integer memoryLimit;

    // Metadata
    private List<String> tags;
    private List<String> companyTags;
    private List<String> hints;
    private String editorial;
    private String editorialCode;
    private Map<String, String> starterCode;

    // Statistics
    private BigDecimal acceptanceRate;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;

    // Status
    private Boolean isPublished;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Sample test cases (visible to all users)
    private List<TestCaseResponse> sampleTestCases;

    // Total test case count (hidden count not revealed)
    private Integer totalTestCases;
}
