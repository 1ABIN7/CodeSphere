package com.CodeSphere.backend.dto.problem;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Compact problem representation for list/browse views.
 * Excludes full descriptions and test cases to keep payloads small.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemListResponse {

    private Long id;
    private String title;
    private String difficulty;
    private BigDecimal acceptanceRate;
    private Integer totalSubmissions;
    private List<String> tags;
    private Boolean isPublished;

    // User-specific: has the current user solved this problem?
    private Boolean solvedByCurrentUser;
}
