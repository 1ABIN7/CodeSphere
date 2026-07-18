package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.service.CandidateReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateReportServiceImpl implements CandidateReportService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentRepository assessmentRepository;

    @Override
    public Map<String, Object> getCandidateReport(Long userId) {
        List<AssessmentSession> sessions = sessionRepository.findByUserId(userId);
        if (sessions.isEmpty()) {
            return Map.of("userId", userId, "message", "No completed sessions for this user");
        }

        List<Map<String, Object>> examScores = new ArrayList<>();
        double totalSum = 0.0;
        int completedCount = 0;

        for (AssessmentSession session : sessions) {
            if ("GRADED".equals(session.getStatus())) {
                Assessment assessment = assessmentRepository.findById(session.getAssessmentId()).orElse(null);
                String title = assessment != null ? assessment.getTitle() : "Unknown Assessment";
                double passingScore = assessment != null && assessment.getPassingScore() != null ? assessment.getPassingScore() : 50.0;

                double score = session.getTotalScore() != null ? session.getTotalScore() : 0.0;
                double maxScore = session.getMaxPossibleScore() != null ? session.getMaxPossibleScore() : 100.0;
                double pct = (score / maxScore) * 100.0;

                // Calculate Rank
                List<AssessmentSession> allSessions = sessionRepository.findByAssessmentId(session.getAssessmentId());
                allSessions.sort((a, b) -> Double.compare(
                        b.getTotalScore() != null ? b.getTotalScore() : 0.0,
                        a.getTotalScore() != null ? a.getTotalScore() : 0.0
                ));
                int rank = 1;
                for (int i = 0; i < allSessions.size(); i++) {
                    if (allSessions.get(i).getUserId().equals(userId)) {
                        rank = i + 1;
                        break;
                    }
                }

                // Performance Band
                String band;
                if (pct >= 90) band = "Excellent (A)";
                else if (pct >= 75) band = "Good (B)";
                else if (pct >= 50) band = "Average (C)";
                else band = "Needs Improvement (D)";

                Map<String, Object> scoreDetails = Map.of(
                        "assessmentId", session.getAssessmentId(),
                        "examTitle", title,
                        "score", score,
                        "maxPossibleScore", maxScore,
                        "percentage", pct,
                        "passingScore", passingScore,
                        "status", pct >= passingScore ? "PASSED" : "FAILED",
                        "rank", rank,
                        "totalParticipants", allSessions.size(),
                        "performanceBand", band
                );
                examScores.add(scoreDetails);
                totalSum += pct;
                completedCount++;
            }
        }

        double averagePercentage = completedCount > 0 ? (totalSum / completedCount) : 0.0;
        String overallBand;
        if (averagePercentage >= 90) overallBand = "Excellent (A)";
        else if (averagePercentage >= 75) overallBand = "Good (B)";
        else if (averagePercentage >= 50) overallBand = "Average (C)";
        else overallBand = "Needs Improvement (D)";

        return Map.of(
                "userId", userId,
                "completedAssessmentsCount", completedCount,
                "averagePercentage", averagePercentage,
                "overallPerformanceBand", overallBand,
                "reportDetails", examScores
        );
    }

    @Override
    public List<Map<String, Object>> getPerformanceTrend(Long userId) {
        List<AssessmentSession> sessions = sessionRepository.findByUserId(userId).stream()
                .filter(s -> "GRADED".equals(s.getStatus()))
                .sorted(Comparator.comparing(s -> s.getSubmittedAt() != null ? s.getSubmittedAt() : s.getStartedAt()))
                .collect(Collectors.toList());

        List<Map<String, Object>> trend = new ArrayList<>();
        for (AssessmentSession session : sessions) {
            Assessment assessment = assessmentRepository.findById(session.getAssessmentId()).orElse(null);
            String title = assessment != null ? assessment.getTitle() : "Unknown Assessment";
            trend.add(Map.of(
                    "assessmentId", session.getAssessmentId(),
                    "examTitle", title,
                    "score", session.getTotalScore() != null ? session.getTotalScore() : 0.0,
                    "maxPossibleScore", session.getMaxPossibleScore() != null ? session.getMaxPossibleScore() : 100.0,
                    "date", session.getSubmittedAt() != null ? session.getSubmittedAt().toString() : ""
            ));
        }
        return trend;
    }
}
