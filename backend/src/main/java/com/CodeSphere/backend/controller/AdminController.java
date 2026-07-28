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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Manually overrides a submission score and logs the action for security oversight.
     */
    @PutMapping("/submissions/{submissionId}/score")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
        return (Long) authentication.getPrincipal();
    }
}
