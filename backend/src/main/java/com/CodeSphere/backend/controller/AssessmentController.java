package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.service.AssessmentService;
import com.CodeSphere.backend.service.AuditLogService;
import com.CodeSphere.backend.repository.AssessmentAssignmentRepository;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.security.CustomUserDetails;
import com.CodeSphere.backend.dto.AssessmentAssignmentStatusDto;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.CandidateGroupRepository;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuditLogService auditLogService;
    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSectionRepository sectionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final UserRepository userRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final CandidateGroupRepository candidateGroupRepository;
    private final NotificationService notificationService;

    // --- Read Operations ---

    @GetMapping
    public ResponseEntity<List<Assessment>> getAllAssessments() {
        return ResponseEntity.ok(assessmentService.getAllAssessments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assessment> getAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssessmentById(id));
    }

    @GetMapping("/{id}/assigned-candidates")
    public ResponseEntity<List<AssessmentAssignment>> getAssignedCandidates(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssignedCandidates(id));
    }

    @GetMapping("/{id}/assignment-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AssessmentAssignmentStatusDto>> getAssignmentStatus(@PathVariable Long id) {
        java.util.Map<Long, AssessmentSession> sessions = assessmentSessionRepository.findByAssessmentId(id).stream()
                .collect(java.util.stream.Collectors.toMap(session -> session.getCandidate().getId(), session -> session, (first, second) -> first));
        return ResponseEntity.ok(assignmentRepository.findByAssessmentId(id).stream().map(assignment -> {
            var user = userRepository.findById(assignment.getUserId()).orElse(null);
            var session = sessions.get(assignment.getUserId());
            return new AssessmentAssignmentStatusDto(assignment.getUserId(), user != null ? user.getUsername() : "Unknown",
                    user != null ? user.getFullName() : null, assignment.getDeadline(), session == null ? "ASSIGNED" : session.getStatus().name(),
                    session != null && session.getSubmittedAt() != null ? session.getSubmittedAt().toLocalDateTime() : null);
        }).toList());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Assessment>> getMyAvailableAssessments() {
        Long userId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        List<Assessment> assessments = assignmentRepository.findByUserId(userId).stream()
                .filter(assignment -> assignment.getDeadline() == null || !assignment.getDeadline().isBefore(now))
                .map(assignment -> assessmentRepository.findById(assignment.getAssessmentId()).orElse(null))
                .filter(assessment -> assessment != null && assessment.isPublished())
                .filter(assessment -> assessment.getStartTime() == null || !assessment.getStartTime().isAfter(now))
                .filter(assessment -> assessment.getEndTime() == null || !assessment.getEndTime().isBefore(now))
                .filter(assessment -> !assessmentSessionRepository.existsByAssessmentIdAndCandidateIdAndStatusIn(
                        assessment.getId(), userId, java.util.List.of(AssessmentSession.SessionStatus.SUBMITTED, AssessmentSession.SessionStatus.TIMED_OUT)))
                .filter(assessment -> sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(assessment.getId()).stream()
                        .anyMatch(section -> !assessmentQuestionRepository.findBySectionIdOrderByOrderIndexAsc(section.getId()).isEmpty()))
                .toList();
        return ResponseEntity.ok(assessments);
    }

    /** Shows a candidate every published assessment assigned to them, including
     * ones that are not yet startable because of a schedule or setup rule. */
    @GetMapping("/assigned")
    public ResponseEntity<List<Assessment>> getMyAssignedAssessments() {
        Long userId = getCurrentUserId();
        List<Assessment> assessments = assignmentRepository.findByUserId(userId).stream()
                .map(assignment -> assessmentRepository.findById(assignment.getAssessmentId()).orElse(null))
                .filter(assessment -> assessment != null && assessment.isPublished())
                .filter(assessment -> !assessmentSessionRepository.existsByAssessmentIdAndCandidateIdAndStatusIn(
                        assessment.getId(), userId, java.util.List.of(AssessmentSession.SessionStatus.SUBMITTED, AssessmentSession.SessionStatus.TIMED_OUT)))
                .toList();
        return ResponseEntity.ok(assessments);
    }

    // --- Admin CRUD & Lifecycle Operations ---

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Assessment> createAssessment(@RequestBody Assessment assessment, HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        Assessment created = assessmentService.createAssessment(assessment);

        auditLogService.logAction(
                adminId,
                "ASSESSMENT_CREATE",
                "Created assessment ID: " + created.getId(),
                request
        );

        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Assessment> updateAssessment(@PathVariable Long id, @RequestBody Assessment assessment) {
        return ResponseEntity.ok(assessmentService.updateAssessment(id, assessment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Void> deleteAssessment(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Assessment> cloneAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.cloneAssessment(id));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Assessment> publishAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.publishAssessment(id));
    }

    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Assessment> unpublishAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.unpublishAssessment(id));
    }

    @PostMapping("/{id}/send-final-scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Assessment> sendFinalScores(@PathVariable Long id) {
        Assessment assessment = assessmentService.getAssessmentById(id);
        assessment.setResultsVisible(true);
        Assessment saved = assessmentRepository.save(assessment);
        assignmentRepository.findByAssessmentId(id).stream()
                .map(AssessmentAssignment::getUserId).distinct()
                .forEach(userId -> notificationService.notify(userId, "Final score available",
                        "Your final score for \"" + saved.getTitle() + "\" is now available."));
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<AssessmentAssignment> assignAssessment(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline) {
        AssessmentAssignment assignment = assessmentService.assignAssessment(id, userId, deadline);
        String title = assessmentRepository.findById(id).map(Assessment::getTitle).orElse("Assessment");
        notificationService.notify(userId, "New assessment assigned", "You have been assigned \"" + title + "\".");
        return ResponseEntity.ok(assignment);
    }

    @PostMapping("/{id}/assign-group")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<List<AssessmentAssignment>> assignAssessmentGroup(@PathVariable Long id, @RequestParam Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline) {
        var group = candidateGroupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Candidate group not found"));
        List<AssessmentAssignment> created = group.getMemberUserIds().stream()
                .filter(userId -> !assignmentRepository.existsByAssessmentIdAndUserId(id, userId))
                .map(userId -> assessmentService.assignAssessment(id, userId, deadline)).toList();
        String title = assessmentRepository.findById(id).map(Assessment::getTitle).orElse("Assessment");
        created.forEach(assignment -> notificationService.notify(assignment.getUserId(), "New assessment assigned", "You have been assigned \"" + title + "\"."));
        return ResponseEntity.ok(created);
    }

    // --- Security Helper ---

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        throw new IllegalStateException("Authenticated user details are unavailable.");
    }
}
