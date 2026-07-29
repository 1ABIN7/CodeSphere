package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.service.AuditLogService;
import com.CodeSphere.backend.service.UserService;
import com.CodeSphere.backend.service.SkillScoreService; // Assumes your score updating logic lives here
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.CodeSphere.backend.service.ScoreService;
import com.CodeSphere.backend.dto.admin.AdminDashboardResponse;
import com.CodeSphere.backend.dto.admin.AssessmentReportResponse;
import com.CodeSphere.backend.dto.admin.QuestionAnalyticsResponse;
import com.CodeSphere.backend.dto.admin.CandidateAnalyticsResponse;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.Problem;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.SubmissionStatus;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.ProblemRepository;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.AssessmentAssignmentRepository;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.McqEvaluationService;
import com.CodeSphere.backend.security.CustomUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ScoreService scoreService;
    private final AuditLogService auditLogService;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final McqEvaluationService mcqEvaluationService;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final QuestionBankRepository questionBankRepository;

    /**
     * Returns real system-wide metrics and the latest candidate submissions for
     * administrators. Individual submission source code is intentionally not
     * included in this lightweight overview.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    public ResponseEntity<AdminDashboardResponse> getDashboard(
            @RequestParam(defaultValue = "6") int activityLimit) {
        int limit = Math.min(Math.max(activityLimit, 1), 25);
        var recentSubmissions = submissionRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        Map<Long, String> problemTitles = problemRepository.findAllById(
                        recentSubmissions.stream().map(Submission::getProblemId).toList())
                .stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle));
        Map<Long, String> candidateNames = userRepository.findAllById(
                        recentSubmissions.stream().map(Submission::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        var activity = recentSubmissions.stream()
                .map(submission -> AdminDashboardResponse.RecentActivity.builder()
                        .id(submission.getId())
                        .problemTitle(problemTitles.getOrDefault(submission.getProblemId(),
                                "Problem #" + submission.getProblemId()))
                        .candidateName(candidateNames.getOrDefault(submission.getUserId(), "Unknown candidate"))
                        .language(submission.getLanguage())
                        .status(submission.getStatus().name())
                        .createdAt(submission.getCreatedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(AdminDashboardResponse.builder()
                .totalAssessments(assessmentRepository.count())
                .pendingReviews(submissionRepository.countByStatus(SubmissionStatus.PENDING))
                .activeSessions(assessmentSessionRepository.countByStatus(AssessmentSession.SessionStatus.IN_PROGRESS))
                .publishedQuestions(problemRepository.countByIsPublishedTrue())
                .recentActivity(activity)
                .build());
    }

    @GetMapping("/reports/assessments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    @Transactional(readOnly = true)
    public ResponseEntity<AssessmentReportResponse> getAssessmentReports() {
        List<AssessmentReportResponse.AssessmentMetric> metrics = assessmentRepository.findAll().stream()
                .map(this::buildAssessmentMetric)
                .toList();
        long candidates = metrics.stream().mapToLong(AssessmentReportResponse.AssessmentMetric::getCandidates).sum();
        double averageScore = average(metrics.stream().mapToDouble(AssessmentReportResponse.AssessmentMetric::getAverageScore).toArray());
        double completionRate = candidates == 0 ? 0 : weightedAverage(metrics, candidates,
                AssessmentReportResponse.AssessmentMetric::getCompletionRate);
        double passRate = candidates == 0 ? 0 : weightedAverage(metrics, candidates,
                AssessmentReportResponse.AssessmentMetric::getPassRate);
        return ResponseEntity.ok(AssessmentReportResponse.builder()
                .summary(AssessmentReportResponse.Summary.builder().averageScore(averageScore)
                        .completionRate(completionRate).passRate(passRate).build())
                .assessments(metrics).build());
    }

    private AssessmentReportResponse.AssessmentMetric buildAssessmentMetric(Assessment assessment) {
        List<AssessmentSession> sessions = assessmentSessionRepository.findByAssessmentId(assessment.getId());
        long candidates = Math.max(sessions.size(), assessmentAssignmentRepository.findByAssessmentId(assessment.getId()).size());
        List<AssessmentSession> completed = sessions.stream().filter(session ->
                session.getStatus() == AssessmentSession.SessionStatus.SUBMITTED || session.getStatus() == AssessmentSession.SessionStatus.TIMED_OUT).toList();
        double totalPossible = assessmentQuestionRepository.findByAssessmentId(assessment.getId()).stream()
                .map(AssessmentQuestion::getMaxScore).filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        double[] scorePercentages = completed.stream().mapToDouble(session -> totalPossible == 0 ? 0 :
                (mcqEvaluationService.evaluateSessionMcqs(session) / totalPossible) * 100).toArray();
        double averageScore = average(scorePercentages);
        double passRate = completed.isEmpty() || assessment.getPassingScore() == null ? 0 :
                (double) java.util.Arrays.stream(scorePercentages).filter(score -> score >= assessment.getPassingScore()).count() / completed.size() * 100;
        return AssessmentReportResponse.AssessmentMetric.builder().id(assessment.getId()).title(assessment.getTitle())
                .candidates(candidates).averageScore(averageScore).passRate(passRate)
                .completionRate(candidates == 0 ? 0 : (double) completed.size() / candidates * 100).build();
    }

    private double average(double[] values) {
        return values.length == 0 ? 0 : java.util.Arrays.stream(values).average().orElse(0);
    }

    private double weightedAverage(List<AssessmentReportResponse.AssessmentMetric> metrics, long totalCandidates,
                                   java.util.function.ToDoubleFunction<AssessmentReportResponse.AssessmentMetric> value) {
        return metrics.stream().mapToDouble(metric -> metric.getCandidates() * value.applyAsDouble(metric)).sum() / totalCandidates;
    }

    @GetMapping("/reports/questions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    @Transactional(readOnly = true)
    public ResponseEntity<QuestionAnalyticsResponse> getQuestionAnalytics() {
        Map<Long, AssessmentSession> sessions = assessmentSessionRepository.findAll().stream()
                .collect(Collectors.toMap(AssessmentSession::getId, session -> session));
        Map<Long, List<com.CodeSphere.backend.model.AssessmentAnswer>> answers = assessmentAnswerRepository.findAll().stream()
                .filter(answer -> answer.getSelectedAnswer() != null && !answer.getSelectedAnswer().isBlank())
                .collect(Collectors.groupingBy(com.CodeSphere.backend.model.AssessmentAnswer::getQuestionId));
        List<com.CodeSphere.backend.model.Question> allQuestions = questionBankRepository.findAll();
        Map<Long, com.CodeSphere.backend.model.Question> questionsById = allQuestions.stream()
                .collect(Collectors.toMap(com.CodeSphere.backend.model.Question::getId, question -> question));
        Map<Long, List<Long>> comprehensionChildren = allQuestions.stream().filter(question -> question.getParentQuestionId() != null)
                .collect(Collectors.groupingBy(com.CodeSphere.backend.model.Question::getParentQuestionId,
                        Collectors.mapping(com.CodeSphere.backend.model.Question::getId, Collectors.toList())));
        var metrics = allQuestions.stream().filter(question -> question.getParentQuestionId() == null).map(question -> {
            List<com.CodeSphere.backend.model.AssessmentAnswer> attempts = "READING_COMPREHENSION".equals(question.getQuestionType())
                    ? comprehensionChildren.getOrDefault(question.getId(), List.of()).stream().flatMap(childId -> answers.getOrDefault(childId, List.of()).stream()).toList()
                    : answers.getOrDefault(question.getId(), List.of());
            long successes = attempts.stream().filter(answer -> isCorrect(questionsById.get(answer.getQuestionId()), answer)).count();
            double averageScore = attempts.isEmpty() ? 0 : attempts.stream()
                    .mapToDouble(answer -> answerScorePercent(questionsById.get(answer.getQuestionId()), answer)).average().orElse(0);
            long averageSeconds = attempts.stream().mapToLong(answer -> {
                AssessmentSession session = sessions.get(answer.getSessionId());
                if (session == null || answer.getUpdatedAt() == null || session.getStartedAt() == null) return 0L;
                return Math.max(0, java.time.Duration.between(session.getStartedAt().toLocalDateTime(), answer.getUpdatedAt()).getSeconds());
            }).average().stream().mapToLong(Math::round).findFirst().orElse(0L);
            return QuestionAnalyticsResponse.QuestionMetric.builder().id(question.getId()).title(question.getTitle())
                    .questionType(question.getQuestionType()).difficulty(question.getDifficulty()).attempts(attempts.size())
                    .successRate(attempts.isEmpty() ? 0 : successes * 100D / attempts.size())
                    .averageScore(averageScore).averageSecondsSpent(averageSeconds).build();
        }).filter(metric -> metric.getAttempts() > 0).toList();
        return ResponseEntity.ok(QuestionAnalyticsResponse.builder().questions(metrics).build());
    }

    private boolean isCorrect(com.CodeSphere.backend.model.Question question, com.CodeSphere.backend.model.AssessmentAnswer answer) {
        if (answer.getScore() != null) return answer.getScore().doubleValue() > 0;
        String submitted = answer.getSelectedAnswer() == null ? "" : answer.getSelectedAnswer().replaceAll("\\s", "").toUpperCase();
        String expected = question.getCorrectAnswers() == null ? "" : question.getCorrectAnswers().replaceAll("\\s", "").toUpperCase();
        return !expected.isBlank() && submitted.equals(expected);
    }

    private double answerScorePercent(com.CodeSphere.backend.model.Question question, com.CodeSphere.backend.model.AssessmentAnswer answer) {
        if (answer.getScore() != null) {
            return question.getPoints() == null || question.getPoints() == 0 ? 0 : Math.max(0, answer.getScore().doubleValue() / question.getPoints() * 100);
        }
        return isCorrect(question, answer) ? 100 : 0;
    }

    @GetMapping("/reports/candidates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    @Transactional(readOnly = true)
    public ResponseEntity<CandidateAnalyticsResponse> getCandidateAnalytics() {
        Map<Long, Assessment> assessments = assessmentRepository.findAll().stream()
                .collect(Collectors.toMap(Assessment::getId, assessment -> assessment));
        Map<Long, Double> totals = assessmentQuestionRepository.findAll().stream()
                .collect(Collectors.groupingBy(AssessmentQuestion::getAssessmentId,
                        Collectors.summingDouble(question -> question.getMaxScore() == null ? 0 : question.getMaxScore())));
        Map<Long, List<AssessmentSession>> candidateSessions = assessmentSessionRepository.findAll().stream()
                .filter(session -> session.getStatus() == AssessmentSession.SessionStatus.SUBMITTED || session.getStatus() == AssessmentSession.SessionStatus.TIMED_OUT)
                .collect(Collectors.groupingBy(session -> session.getCandidate().getId()));
        var unsorted = candidateSessions.entrySet().stream().map(entry -> {
            List<CandidateAnalyticsResponse.TrendPoint> trend = entry.getValue().stream()
                    .sorted(java.util.Comparator.comparing(AssessmentSession::getSubmittedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .map(session -> {
                        double earned = mcqEvaluationService.evaluateSessionMcqs(session) + assessmentAnswerRepository.findBySessionId(session.getId()).stream()
                                .filter(answer -> "EVALUATED".equals(answer.getEvaluationStatus()) && answer.getScore() != null)
                                .mapToDouble(answer -> answer.getScore().doubleValue()).sum();
                        double total = totals.getOrDefault(session.getAssessmentId(), 0D);
                        Assessment assessment = assessments.get(session.getAssessmentId());
                        return CandidateAnalyticsResponse.TrendPoint.builder().completedAt(session.getSubmittedAt())
                                .assessmentTitle(assessment == null ? "Assessment" : assessment.getTitle())
                                .score(total == 0 ? 0 : earned / total * 100).build();
                    }).toList();
            User candidate = entry.getValue().getFirst().getCandidate();
            double average = trend.stream().mapToDouble(CandidateAnalyticsResponse.TrendPoint::getScore).average().orElse(0);
            return CandidateAnalyticsResponse.CandidateMetric.builder().userId(candidate.getId())
                    .name(candidate.getFullName() == null || candidate.getFullName().isBlank() ? candidate.getUsername() : candidate.getFullName())
                    .completedAssessments(trend.size()).averageScore(average).trend(trend).build();
        }).sorted(java.util.Comparator.comparing(CandidateAnalyticsResponse.CandidateMetric::getAverageScore).reversed()).toList();
        var ranked = new java.util.ArrayList<CandidateAnalyticsResponse.CandidateMetric>();
        for (int index = 0; index < unsorted.size(); index++) {
            var candidate = unsorted.get(index);
            ranked.add(CandidateAnalyticsResponse.CandidateMetric.builder().userId(candidate.getUserId()).name(candidate.getName())
                    .rank(index + 1).completedAssessments(candidate.getCompletedAssessments()).averageScore(candidate.getAverageScore()).trend(candidate.getTrend()).build());
        }
        return ResponseEntity.ok(CandidateAnalyticsResponse.builder().candidates(ranked).build());
    }

    /**
     * Manually overrides a submission score and logs the action for security oversight.
     */
    @PutMapping("/submissions/{submissionId}/score")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    public ResponseEntity<Void> updateScore(@PathVariable Long submissionId,
                                            @RequestParam Double newScore,
                                            HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        scoreService.updateSubmissionScore(submissionId, newScore);

        // [Audit Log] Log Score Change
        auditLogService.logAction(
            adminId,
            "SCORE-CHANGE",
            String.format("Submission ID: %d | New Score: %.2f", submissionId, newScore),
            request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Updates privileges/roles for a target system user and captures the event in the audit index.
     */
    @PutMapping("/users/{targetUserId}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Void> changeUserRole(@PathVariable Long targetUserId,
                                               @RequestParam String newRole,
                                               HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        userService.updateUserRole(targetUserId, newRole);

        // [Audit Log] Log Role Modification
        auditLogService.logAction(
            adminId,
            "USER-ROLE-CHANGE",
            String.format("User ID: %d | Role Assigned: %s", targetUserId, newRole),
            request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Extract the administrative user ID out from the current security framework thread.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails user) return user.getId();
        throw new IllegalStateException("Authenticated user details are unavailable.");
    }
}
