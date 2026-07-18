package com.CodeSphere.backend.service;

import java.util.Map;

public interface QuestionAnalyticsService {
    Map<String, Object> getQuestionAnalytics(Long questionId);
    Map<String, Object> getDifficultyAnalysis(Long assessmentId);
}
