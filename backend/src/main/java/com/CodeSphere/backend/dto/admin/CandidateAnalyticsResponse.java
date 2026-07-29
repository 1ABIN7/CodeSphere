package com.CodeSphere.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CandidateAnalyticsResponse {
    private final List<CandidateMetric> candidates;

    @Getter @Builder @AllArgsConstructor
    public static class CandidateMetric {
        private final Long userId;
        private final String name;
        private final int rank;
        private final long completedAssessments;
        private final double averageScore;
        private final List<TrendPoint> trend;
    }

    @Getter @Builder @AllArgsConstructor
    public static class TrendPoint {
        private final OffsetDateTime completedAt;
        private final String assessmentTitle;
        private final double score;
    }
}
