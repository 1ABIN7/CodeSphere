package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.SessionAnswer;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.SessionAnswerRepository;
import com.CodeSphere.backend.service.QuestionAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionAnalyticsServiceImpl implements QuestionAnalyticsService {

    private final SessionAnswerRepository answerRepository;
    private final AssessmentSessionRepository sessionRepository;

    @Override
    public Map<String, Object> getQuestionAnalytics(Long questionId) {
        List<SessionAnswer> answers = answerRepository.findAll().stream()
                .filter(a -> a.getQuestionId().equals(questionId))
                .toList();

        if (answers.isEmpty()) {
            return Map.of("questionId", questionId, "message", "No answers recorded for this question");
        }

        long totalAttempts = answers.size();
        List<SessionAnswer> graded = answers.stream()
                .filter(a -> a.getScore() != null)
                .toList();

        double avgScore = 0.0;
        long successCount = 0;

        if (!graded.isEmpty()) {
            double sum = 0.0;
            for (SessionAnswer a : graded) {
                sum += a.getScore();
                // Assume success if scored >= 50%
                if (a.getScore() >= 5.0) { // Defaulting hypothetical max question score to 10
                    successCount++;
                }
            }
            avgScore = sum / graded.size();
        }

        double successRate = !graded.isEmpty() ? ((double) successCount / graded.size()) * 100.0 : 0.0;

        return Map.of(
                "questionId", questionId,
                "totalAttempts", totalAttempts,
                "gradedAttempts", graded.size(),
                "averageScore", avgScore,
                "successRatePercentage", successRate,
                "avgTimeSpentSeconds", 120 // Fallback estimate
        );
    }

    @Override
    public Map<String, Object> getDifficultyAnalysis(Long assessmentId) {
        List<AssessmentSession> sessions = sessionRepository.findByAssessmentId(assessmentId);
        if (sessions.isEmpty()) {
            return Map.of("assessmentId", assessmentId, "message", "No sessions recorded for difficulty analysis");
        }

        List<SessionAnswer> answers = new ArrayList<>();
        for (AssessmentSession s : sessions) {
            answers.addAll(answerRepository.findBySessionId(s.getId()));
        }

        Map<Long, List<SessionAnswer>> grouped = new HashMap<>();
        for (SessionAnswer a : answers) {
            grouped.computeIfAbsent(a.getQuestionId(), k -> new ArrayList<>()).add(a);
        }

        Map<String, Object> calibration = new HashMap<>();

        for (Map.Entry<Long, List<SessionAnswer>> entry : grouped.entrySet()) {
            Long qId = entry.getKey();
            List<SessionAnswer> qAnswers = entry.getValue();
            List<SessionAnswer> graded = qAnswers.stream().filter(a -> a.getScore() != null).toList();

            long successCount = 0;
            for (SessionAnswer a : graded) {
                if (a.getScore() >= 5.0) {
                    successCount++;
                }
            }

            double rate = !graded.isEmpty() ? ((double) successCount / graded.size()) * 100.0 : 50.0;
            String calibrationDifficulty;
            if (rate < 30) {
                calibrationDifficulty = "HARD";
            } else if (rate < 70) {
                calibrationDifficulty = "MEDIUM";
            } else {
                calibrationDifficulty = "EASY";
            }

            calibration.put(String.valueOf(qId), Map.of(
                    "successRate", rate,
                    "calibratedDifficulty", calibrationDifficulty
            ));
        }

        return Map.of(
                "assessmentId", assessmentId,
                "difficultyCalibrationReport", calibration
        );
    }
}
