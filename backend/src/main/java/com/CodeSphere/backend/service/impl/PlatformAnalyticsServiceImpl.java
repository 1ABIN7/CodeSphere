package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.PlatformAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAnalyticsServiceImpl implements PlatformAnalyticsService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final AssessmentSessionRepository sessionRepository;

    @Override
    public Map<String, Object> getPlatformAnalytics() {
        long activeUsersCount = userRepository.count();
        long totalSubmissionsCount = submissionRepository.count();
        long totalSessionsCount = sessionRepository.count();

        // Pass rate calculation
        List<AssessmentSession> gradedSessions = sessionRepository.findAll().stream()
                .filter(s -> "GRADED".equals(s.getStatus()))
                .toList();

        double globalPassPercentage = 0.0;
        if (!gradedSessions.isEmpty()) {
            long passCount = 0;
            for (AssessmentSession s : gradedSessions) {
                double score = s.getTotalScore() != null ? s.getTotalScore() : 0.0;
                double maxScore = s.getMaxPossibleScore() != null ? s.getMaxPossibleScore() : 100.0;
                double pct = (score / maxScore) * 100.0;
                if (pct >= 50.0) { // Assuming global default pass mark = 50%
                    passCount++;
                }
            }
            globalPassPercentage = ((double) passCount / gradedSessions.size()) * 100.0;
        }

        // Language trends
        List<Submission> submissions = submissionRepository.findAll();
        Map<String, Long> languageTrends = submissions.stream()
                .filter(s -> s.getLanguage() != null)
                .collect(Collectors.groupingBy(Submission::getLanguage, Collectors.counting()));

        return Map.of(
                "activeUsersCount", activeUsersCount,
                "totalCodeSubmissions", totalSubmissionsCount,
                "totalAssessmentSessions", totalSessionsCount,
                "globalPassPercentage", globalPassPercentage,
                "languageTrends", languageTrends
        );
    }
}
