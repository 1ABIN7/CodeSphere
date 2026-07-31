package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.admin.CandidateAiInsightResponse;
import com.CodeSphere.backend.dto.admin.CandidateSkillMetricsResponse;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.CodingAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai-insights")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
public class AdminAiInsightsController {
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final CodingAssistantService codingAssistantService;

    @GetMapping
    public ResponseEntity<CandidateAiInsightResponse> candidateInsight(@RequestParam Long candidateId) {
        User candidate = candidate(candidateId);
        List<Submission> submissions = recentSubmissions(candidateId);
        if (submissions.isEmpty()) throw new IllegalArgumentException("This candidate has no coding submissions to analyze yet.");
        String name = candidateName(candidate);
        String insight = codingAssistantService.evaluateCandidateSkills(name, summarize(submissions));
        return ResponseEntity.ok(new CandidateAiInsightResponse(name, submissions.size(), insight));
    }

    @GetMapping("/metrics")
    public ResponseEntity<CandidateSkillMetricsResponse> metrics(@RequestParam Long candidateId) {
        User candidate = candidate(candidateId);
        List<Submission> newestFirst = recentSubmissions(candidateId);
        List<Submission> oldestFirst = new java.util.ArrayList<>(newestFirst);
        java.util.Collections.reverse(oldestFirst);
        Map<String, int[]> languageCounts = new LinkedHashMap<>();
        Map<String, Integer> verdictCounts = new LinkedHashMap<>();
        List<CandidateSkillMetricsResponse.ScoreTrendPoint> trend = new java.util.ArrayList<>();
        int accepted = 0;
        for (int index = 0; index < oldestFirst.size(); index++) {
            Submission submission = oldestFirst.get(index);
            boolean passed = submission.getStatus() == com.CodeSphere.backend.model.SubmissionStatus.ACCEPTED;
            if (passed) accepted++;
            int[] language = languageCounts.computeIfAbsent(submission.getLanguage() == null || submission.getLanguage().isBlank() ? "Unknown" : submission.getLanguage(), ignored -> new int[2]);
            language[0]++;
            if (passed) language[1]++;
            String verdict = submission.getStatus() == null ? "UNKNOWN" : submission.getStatus().name();
            verdictCounts.merge(verdict, 1, Integer::sum);
            int total = submission.getTotalTestCases() == null ? 0 : submission.getTotalTestCases();
            int passedTests = submission.getTestCasesPassed() == null ? 0 : submission.getTestCasesPassed();
            int score = total > 0 ? (int) Math.round((double) passedTests * 100 / total) : (passed ? 100 : 0);
            trend.add(new CandidateSkillMetricsResponse.ScoreTrendPoint("#" + (index + 1), score));
        }
        List<CandidateSkillMetricsResponse.LanguageMetric> languages = languageCounts.entrySet().stream()
                .map(entry -> new CandidateSkillMetricsResponse.LanguageMetric(entry.getKey(), entry.getValue()[0], entry.getValue()[1])).toList();
        List<CandidateSkillMetricsResponse.StatusMetric> verdicts = verdictCounts.entrySet().stream()
                .map(entry -> new CandidateSkillMetricsResponse.StatusMetric(entry.getKey().replace('_', ' '), entry.getValue())).toList();
        return ResponseEntity.ok(new CandidateSkillMetricsResponse(candidateName(candidate), newestFirst.size(), accepted, trend, languages, verdicts));
    }

    private User candidate(Long candidateId) {
        User candidate = userRepository.findById(candidateId).orElseThrow(() -> new IllegalArgumentException("Candidate not found."));
        if (candidate.getRole() != com.CodeSphere.backend.model.Role.ROLE_CANDIDATE) throw new IllegalArgumentException("Choose a candidate account.");
        return candidate;
    }

    private List<Submission> recentSubmissions(Long candidateId) {
        return submissionRepository.findByUserIdOrderByCreatedAtDesc(candidateId, PageRequest.of(0, 12)).getContent();
    }

    private String candidateName(User candidate) {
        return candidate.getFullName() == null || candidate.getFullName().isBlank() ? candidate.getUsername() : candidate.getFullName();
    }

    private String summarize(List<Submission> submissions) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < submissions.size(); index++) {
            Submission submission = submissions.get(index);
            String code = submission.getCode() == null ? "" : submission.getCode().replaceAll("\\s+$", "");
            if (code.length() > 900) code = code.substring(0, 900) + "\n[excerpt truncated]";
            result.append("Submission ").append(index + 1)
                    .append(" | language: ").append(submission.getLanguage())
                    .append(" | verdict: ").append(submission.getStatus())
                    .append(" | tests: ").append(submission.getTestCasesPassed()).append('/').append(submission.getTotalTestCases())
                    .append("\nCode excerpt:\n").append(code).append("\n\n");
        }
        return result.toString();
    }
}
