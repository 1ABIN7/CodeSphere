package com.CodeSphere.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuestionAnalyticsResponse {
    private final List<QuestionMetric> questions;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class QuestionMetric {
        private final Long id;
        private final String title;
        private final String questionType;
        private final long attempts;
        private final double successRate;
        private final double averageScore;
        private final long averageSecondsSpent;
    }
}
