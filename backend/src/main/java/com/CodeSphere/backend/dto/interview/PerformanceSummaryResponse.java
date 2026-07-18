package com.CodeSphere.backend.dto.interview;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceSummaryResponse {
    private Long userId;
    private Integer totalSessionsCompleted;
    private Integer totalQuestionsAttempted;
    private Double overallAccuracy;
    private List<CategoryStatsResponse> categoryStats;
}
