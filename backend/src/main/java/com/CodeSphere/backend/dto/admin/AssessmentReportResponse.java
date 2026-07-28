package com.CodeSphere.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AssessmentReportResponse {
    private final Summary summary;
    private final List<AssessmentMetric> assessments;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private final double averageScore;
        private final double completionRate;
        private final double passRate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AssessmentMetric {
        private final Long id;
        private final String title;
        private final long candidates;
        private final double averageScore;
        private final double passRate;
        private final double completionRate;
    }
}
