package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.AssessmentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AssessmentReportServiceImpl implements AssessmentReportService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final ExamReportGenerator reportGenerator;

    @Override
    public Map<String, Object> getAssessmentReport(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));

        List<AssessmentSession> sessions = sessionRepository.findByAssessmentId(assessmentId);
        if (sessions.isEmpty()) {
            return Map.of("assessmentId", assessmentId, "message", "No sessions recorded for this assessment");
        }

        int participationCount = sessions.size();
        long completionCount = sessions.stream()
                .filter(s -> List.of("SUBMITTED", "GRADING", "GRADED").contains(s.getStatus()))
                .count();

        double completionRate = participationCount > 0 ? ((double) completionCount / participationCount) * 100.0 : 0.0;

        List<AssessmentSession> gradedSessions = sessions.stream()
                .filter(s -> "GRADED".equals(s.getStatus()))
                .toList();

        double averageScore = 0.0;
        double passingScore = assessment.getPassingScore() != null ? assessment.getPassingScore() : 50.0;
        long passCount = 0;

        if (!gradedSessions.isEmpty()) {
            double sum = 0.0;
            for (AssessmentSession s : gradedSessions) {
                double score = s.getTotalScore() != null ? s.getTotalScore() : 0.0;
                double maxScore = s.getMaxPossibleScore() != null ? s.getMaxPossibleScore() : 100.0;
                double pct = (score / maxScore) * 100.0;

                sum += score;
                if (pct >= passingScore) {
                    passCount++;
                }
            }
            averageScore = sum / gradedSessions.size();
        }

        double passPercentage = !gradedSessions.isEmpty() ? ((double) passCount / gradedSessions.size()) * 100.0 : 0.0;

        // Top Performers list
        List<Map<String, Object>> candidateDetails = getCandidateDetails(gradedSessions, passingScore);
        // Sort top performers by score desc
        List<Map<String, Object>> sortedDetails = new ArrayList<>(candidateDetails);
        sortedDetails.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        
        List<Map<String, Object>> topPerformers = sortedDetails.stream().limit(5).toList();

        Map<String, Object> report = new HashMap<>();
        report.put("assessmentId", assessmentId);
        report.put("examTitle", assessment.getTitle());
        report.put("participationCount", participationCount);
        report.put("completionRate", completionRate);
        report.put("averageScore", averageScore);
        report.put("passPercentage", passPercentage);
        report.put("topPerformers", topPerformers);

        return report;
    }

    @Override
    public byte[] exportAssessmentReportCSV(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
        
        List<AssessmentSession> gradedSessions = sessionRepository.findByAssessmentId(assessmentId).stream()
                .filter(s -> "GRADED".equals(s.getStatus()))
                .toList();
        
        double passingScore = assessment.getPassingScore() != null ? assessment.getPassingScore() : 50.0;
        List<Map<String, Object>> candidateDetails = getCandidateDetails(gradedSessions, passingScore);

        StringBuilder csv = new StringBuilder("User ID,Name,Email,Score,Max Score,Percentage,Status\n");
        for (Map<String, Object> candidate : candidateDetails) {
            csv.append(candidate.get("userId")).append(",")
               .append(candidate.get("name")).append(",")
               .append(candidate.get("email")).append(",")
               .append(candidate.get("score")).append(",")
               .append(candidate.get("maxPossibleScore")).append(",")
               .append(String.format("%.2f%%", candidate.get("percentage"))).append(",")
               .append(candidate.get("status")).append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportAssessmentReportPDF(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));

        Map<String, Object> reportStats = getAssessmentReport(assessmentId);
        
        List<AssessmentSession> gradedSessions = sessionRepository.findByAssessmentId(assessmentId).stream()
                .filter(s -> "GRADED".equals(s.getStatus()))
                .toList();
        double passingScore = assessment.getPassingScore() != null ? assessment.getPassingScore() : 50.0;
        List<Map<String, Object>> candidateDetails = getCandidateDetails(gradedSessions, passingScore);

        return reportGenerator.generatePDFReport(
                "CodeSphere Assessment Report - " + assessment.getTitle(),
                reportStats,
                candidateDetails
        );
    }

    private List<Map<String, Object>> getCandidateDetails(List<AssessmentSession> gradedSessions, double passingScore) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (AssessmentSession s : gradedSessions) {
            User user = userRepository.findById(s.getUserId()).orElse(null);
            String name = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Unknown";
            String email = user != null ? user.getEmail() : "N/A";

            double score = s.getTotalScore() != null ? s.getTotalScore() : 0.0;
            double maxScore = s.getMaxPossibleScore() != null ? s.getMaxPossibleScore() : 100.0;
            double pct = (score / maxScore) * 100.0;

            details.add(Map.of(
                    "userId", s.getUserId(),
                    "name", name,
                    "email", email,
                    "score", score,
                    "maxPossibleScore", maxScore,
                    "percentage", pct,
                    "status", pct >= passingScore ? "PASSED" : "FAILED"
            ));
        }
        return details;
    }
}
