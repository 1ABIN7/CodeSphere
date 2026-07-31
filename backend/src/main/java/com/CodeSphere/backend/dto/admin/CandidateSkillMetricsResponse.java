package com.CodeSphere.backend.dto.admin;

import java.util.List;

public record CandidateSkillMetricsResponse(
        String candidateName,
        int analyzedSubmissions,
        int acceptedSubmissions,
        List<ScoreTrendPoint> scoreTrend,
        List<LanguageMetric> languages,
        List<StatusMetric> verdicts
) {
    public record ScoreTrendPoint(String label, int score) { }
    public record LanguageMetric(String language, int submitted, int accepted) { }
    public record StatusMetric(String status, int count) { }
}
